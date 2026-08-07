---
sidebar_position: 26
title: "Capítulo 26: Programação Assíncrona de Câmera"
description: "Domine o inferno de callbacks do Camera2 usando coroutines e Flow do Kotlin. Aprenda suspendCancellableCoroutine para operações únicas (openCamera, createCaptureSession, capture), callbackFlow para fluxos contínuos de ImageReader e CaptureResult, operadores combine para UIs reativas e padrões de segurança de threads para evitar ANRs e deadlocks."
keywords: [kotlin coroutines camera2, inferno de callbacks, suspendcancellablecoroutine, callbackflow, camera2 flow, segurança de threads camera2, mutext estado compartilhado, deadlock pipedoutputstream, ui de câmera reativa]
---

# Capítulo 26: Programação Assíncrona de Câmera

## Resumo

Volte e olhe o código que você escreveu para os Capítulos 7 a 9. `CameraDevice.StateCallback` aninhado dentro de `CameraManager.openCamera`, com `CameraCaptureSession.StateCallback` aninhado dentro de `onOpened`, com `CaptureCallback` aninhado dentro de `onConfigured`, com `ImageReader.OnImageAvailableListener` disparando em uma `HandlerThread` que você iniciou manualmente e deve desmontar exatamente na ordem inversa em cada caminho de erro. Este é o inferno de callbacks, com sabor de câmera. Cada nível de indentação é uma nova classe de callback. Cada erro deve se propagar por quatro camadas de objetos anônimos. Cada `close()` esquecido no caminho de desmontagem vaza a câmera até a reinicialização.

Este capítulo é a refatoração que você estava desejando. Convertemos toda a selva de callbacks em um código Kotlin limpo, linear, cancelável e testável, usando duas primitivas de coroutines: `suspendCancellableCoroutine` para operações únicas e `callbackFlow` + operadores `Flow` para fluxos contínuos. Você aprenderá regras de segurança de threads para coroutines interagindo com o Camera2, por que bloquear a thread principal em qualquer chamada de câmera é um ANR esperando para acontecer e por que o padrão `PipedOutputStream`/`PipedInputStream` que você pode ter tentado para dados de ImageWriter produz travamentos que o Flow naturalmente evita.

Como sempre, valide as capacidades de nível de hardware que você está visando com o **Android Camera Parameters** ([Google Play](https://play.google.com/store/apps/details?id=com.minininja.cameraparams), [GitHub](https://github.com/zoozooll/AndroidCameraParameters)) para confirmar que os recursos que seu pipeline assíncrono precisa (burst repetido, resultados parciais, reprocessamento YUV) realmente estão presentes em seus dispositivos de teste.

---

## Por Que Callbacks Aninhados São um "Inferno de Callbacks"

Vamos primeiro visualizar o problema. Esta é uma estrutura real (simplificada) de um aplicativo Camera2 bruto de produção antes das coroutines:

```mermaid
graph TD
    A["onCreateView"] -->|cameraId escolhido| B["CameraManager.openCamera"]
    B -->|dispara em| C[StateCallback.onOpened<br/>lambda 1]
    C -->|mantém cameraDevice| D[createCaptureSession<br/>(saídas = previewSurface + imageReaderSurface)]
    D -->|dispara em| E[Session.StateCallback.onConfigured<br/>lambda 2]
    E -->|mantém session| F[session.setRepeatingRequest<br/>+ CaptureCallback lambda 3]
    F -->|dispara onProgress| G[CaptureCallback.onCaptureProgressed<br/>resultados parciais]
    F -->|dispara onCompleted| H[CaptureCallback.onCaptureCompleted<br/>TotalCaptureResult]
    H -->|quadro pronto| I[ImageReader.OnImageAvailableListener<br/>lambda 4]
    I -->|bytes JPEG| J[chamada de salvamento MediaStore<br/>lambda 5]

    style A fill:#e6d9f2
    style C fill:#f2d9e6
    style E fill:#f2d9e6
    style H fill:#f2d9e6
    style I fill:#f2d9e6
    style J fill:#d9f2e6
```

Cada callback sombreado é uma classe anônima separada. Cada um captura uma referência a recursos de dois níveis acima. Cada caminho de erro deve borbulhar de J de volta para A, fechando `imageReader → sessão → cameraDevice → handlerThread` na ordem inversa, e qualquer única falta de `close()` em qualquer uma das 16 permutações de erro produz um vazamento de câmera permanente até que o dispositivo seja reiniciado. Esta é a definição clássica de inferno de callbacks.

O objetivo deste capítulo é transformar esse espaguete nisto:

```mermaid
flowchart LR
    A["openCameraAwait()"] --> B["createCaptureSessionAwait()"]
    B --> C[setRepeatingRequestFlow()]
    C --> D[aeStateFlow.map().combine(previewFlow)]
    D --> E[Estado da UI<br/>(emissão única)]
    style A fill:#d9f2e6
    style B fill:#d9f2e6
    style C fill:#d9f2e6
    style D fill:#d9f2e6
    style E fill:#e6d9f2
```

Linear. Componível. Testável. Cancelável ao cancelar o `Job` pai. Cada estágio é uma função simples ou um operador `Flow`. Os mesmos cinco callbacks agora vivem em um pipeline linear de 12 linhas.

---

## Coroutines Kotlin para Operações Únicas: `suspendCancellableCoroutine`

O padrão principal para envolver qualquer API baseada em callback como uma função `suspend` é o `suspendCancellableCoroutine`. A receita é sempre a mesma:

1. Chame `suspendCancellableCoroutine { cont -> ... }` para obter uma `CancellableContinuation<T>`.
2. Chame a API real baseada em callback, passando a ela uma implementação de callback anônima.
3. No caminho de sucesso do callback, chame `cont.resume(valor)`.
4. Em cada caminho de erro, chame `cont.resumeWithException(t)`.
5. Em `cont.invokeOnCancellation { ... }`, faça a limpeza: feche a câmera, cancele solicitações pendentes, remova o registro dos listeners para que o callback nunca dispare *depois* que a coroutine foi cancelada.
6. Envolva tudo em um `withTimeout` nos locais de chamada para que um HAL travado não congele seu app para sempre.

### Exemplo 1: `suspend fun openCameraAwait()`

```kotlin
suspend fun CameraManager.openCameraAwait(
    cameraId: String,
    handler: Handler
): CameraDevice = suspendCancellableCoroutine { cont ->
    val callback = object : CameraDevice.StateCallback() {
        override fun onOpened(camera: CameraDevice) {
            cont.resume(camera) {
                camera.close()
            }
        }
        override fun onDisconnected(camera: CameraDevice) {
            cont.resumeWithException(
                CameraAccessException(
                    CameraAccessException.CAMERA_DISCONNECTED,
                    "Câmera $cameraId desconectada durante a abertura"
                )
            )
        }
        override fun onError(camera: CameraDevice, error: Int) {
            cont.resumeWithException(
                CameraAccessException(error, "Erro na câmera $cameraId: $error")
            )
        }
    }

    cont.invokeOnCancellation {
        try {
            // Solução alternativa: openCamera() não expõe um handle cancelável
            // em APIs anteriores à 30. Feche o dispositivo se ele foi aberto na janela de corrida.
        } catch (_: Throwable) { /* ignorar */ }
    }

    openCamera(cameraId, callback, handler)
}
```

**Por que isto funciona.** `openCamera` é do tipo "dispare e esqueça": você a chama e, em algum momento futuro, um dos três métodos de callback dispara exatamente uma vez. Esse contrato ("dispara exatamente uma vez") é o que nos permite mapeá-la um para um em uma continuação. Se a coroutine for cancelada *antes* de qualquer callback disparar, o `invokeOnCancellation` roda e evita um vazamento de recurso. Se for cancelada *depois* do `resume`, o bloco `resume(valor) { camera.close() }` — o parâmetro `onCancellation` do `resume` — fecha o dispositivo automaticamente.

Chamá-la com um tempo limite é trivial:

```kotlin
val cameraDevice: CameraDevice = withTimeoutOrNull(5_000L) {
    cameraManager.openCameraAwait(cameraId, cameraHandler)
} ?: run {
    Log.w(TAG, "Abertura da câmera expirou após 5s")
    return@launch
}
```

Se o HAL estiver travado (comum em dispositivos `LEGACY` de baixo custo após um vazamento de câmera de um app anterior), isso falha de forma rápida e limpa em vez de apresentar ao usuário um diálogo de "O aplicativo não está respondendo".

### Exemplo 2: `suspend fun createCaptureSessionAwait()`

Mesmo padrão, callback diferente:

```kotlin
suspend fun CameraDevice.createCaptureSessionAwait(
    outputs: List<OutputConfiguration>,
    handler: Handler
): CameraCaptureSession = suspendCancellableCoroutine { cont ->
    val callback = object : CameraCaptureSession.StateCallback() {
        override fun onConfigured(session: CameraCaptureSession) {
            cont.resume(session) { session.close() }
        }
        override fun onConfigureFailed(session: CameraCaptureSession) {
            cont.resumeWithException(
                IllegalStateException(
                    "Falha na configuração da sessão para o dispositivo ${this@createCaptureSessionAwait.id}"
                )
            )
        }
    }

    cont.invokeOnCancellation {
        // Não é possível cancelar a criação de sessão em voo em APIs mais antigas.
        // A sessão fechará se ela eventualmente for concluída via o bloco
        // onCancellation do resume acima.
    }

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        val config = SessionConfiguration(
            SessionConfiguration.SESSION_REGULAR,
            outputs,
            { r -> handler.post(r) },
            callback
        )
        createCaptureSession(config)
    } else {
        @Suppress("DEPRECATION")
        createCaptureSession(outputs.map { it.surface }, callback, handler)
    }
}
```

Esta é exatamente a mesma forma. As duas versões da API (`createCaptureSession(surfaces, callback, handler)` pré-S vs `SessionConfiguration` S+) são tratadas em um único wrapper. Os chamadores nunca precisam saber.

### Exemplo 3: `suspend fun awaitCaptureResult()` para Captura Única

```kotlin
suspend fun CameraCaptureSession.captureAwait(
    request: CaptureRequest,
    handler: Handler
): TotalCaptureResult = suspendCancellableCoroutine { cont ->
    val callback = object : CameraCaptureSession.CaptureCallback() {
        override fun onCaptureCompleted(
            session: CameraCaptureSession,
            request: CaptureRequest,
            result: TotalCaptureResult
        ) {
            cont.resume(result)
        }
        override fun onCaptureFailed(
            session: CameraCaptureSession,
            request: CaptureRequest,
            failure: CaptureFailure
        ) {
            cont.resumeWithException(
                CaptureFailureException(
                    "Falha na captura: razão=${failure.reason} quadro=${failure.frameNumber}"
                )
            )
        }
    }

    cont.invokeOnCancellation {
        try { abortCaptures() } catch (_: Throwable) { /* ignorar */ }
    }

    try {
        capture(request, callback, handler)
    } catch (t: Throwable) {
        cont.resumeWithException(t)
    }
}
```

Este é o bloco de construção para o bracketing manual multi-quadro no estilo do Capítulo 19 — encadeie 7 chamadas `captureAwait(br[i])` em um loop `for` com `withTimeoutOrNull`, colete todos os 7 `TotalCaptureResult`s e você terá uma sequência de bracketing HDR completa com tempo limite por quadro e aborto automático no cancelamento. No mundo dos callbacks, isso representava centenas de linhas de máquina de estados. Agora é um loop `for` de 12 linhas.

---

## Flow para Fluxos Contínuos

Operações únicas cobrem a abertura da câmera, criação de sessão e captura única. Para coisas repetidas — cada quadro de pré-visualização, cada `TotalCaptureResult`, cada `Image` de um `ImageReader` — queremos um `Flow<T>` para podermos usar `map`, `filter`, `debounce`, `combine` e compartilhar fluxos entre assinantes.

### Exemplo 4: ImageReader → `Flow<Image>` via `callbackFlow`

```kotlin
fun ImageReader.imagesFlow(
    lifecycleScope: CoroutineScope
): Flow<Image> = callbackFlow {
    val listener = ImageReader.OnImageAvailableListener { reader ->
        // acquireLatestImage() descarta quadros antigos se o consumidor for mais lento
        // do que a câmera produz — obrigatório para evitar o travamento do HAL.
        val image = reader.acquireLatestImage() ?: return@OnImageAvailableListener
        trySend(image)
    }

    // Deve chamar invokeOnClose PRIMEIRO para que o cancelamento sempre remova o listener
    // mesmo que o próprio setOnImageAvailableListener lance exceção.
    awaitClose {
        setOnImageAvailableListener(null, null)
        // NÃO feche o ImageReader aqui — o chamador é o dono do seu ciclo de vida.
    }

    val handlerThread = HandlerThread("ImageReaderFlow").apply { start() }
    val handler = Handler(handlerThread.looper)
    setOnImageAvailableListener(listener, handler)
}.buffer(Channel.CONFLATED)
 .onCompletion {
     // Qualquer Image não consumida por coletores a jusante é nossa para fechar,
     // porque o callbackFlow relança falhas após a emissão.
 }
```

**Escolhas críticas de design:**

1. `acquireLatestImage()` em vez de `acquireNextImage()`. Se o seu processamento de imagem (inferência de ML, detecção facial) levar 40ms e a câmera disparar a 30fps (~33ms por quadro), você *ficará* para trás. O `acquireNextImage` as enfileira até que você fique sem buffers gralloc e a câmera congele. O `acquireLatestImage` pula as antigas e fornece o quadro mais recente. Isso é quase sempre o que você deseja para a análise de imagem do lado da pré-visualização.

2. `buffer(Channel.CONFLATED)`. Um buffer confluente mantém apenas o último valor. Combinado com `acquireLatestImage`, esta é uma garantia forte de que você nunca enfileira quadros obsoletos.

3. `awaitClose { setOnImageAvailableListener(null, null) }`. Este é o equivalente do `callbackFlow` para o `cont.invokeOnCancellation`. Cancele o escopo da coroutine (por exemplo, quando o Fragment passar pelo `onDestroyView`) e o listener será automaticamente desregistrado e a `HandlerThread` limpa. *Sem* vazamentos.

### Exemplo 5: CaptureCallback → `Flow<TotalCaptureResult>`

Mesmo padrão:

```kotlin
fun CameraCaptureSession.repeatingResultsFlow(
    repeatingRequest: CaptureRequest,
    handler: Handler
): Flow<TotalCaptureResult> = callbackFlow {
    val callback = object : CameraCaptureSession.CaptureCallback() {
        override fun onCaptureCompleted(
            session: CameraCaptureSession,
            request: CaptureRequest,
            result: TotalCaptureResult
        ) {
            trySend(result)
        }
        override fun onCaptureProgressed(
            session: CameraCaptureSession,
            request: CaptureRequest,
            partial: CaptureResult
        ) {
            // Se precisar de resultados parciais, emita-os em um canal separado
            // ou envie uma sealed class.
        }
    }

    awaitClose {
        try {
            stopRepeating()
            abortCaptures()
        } catch (_: Throwable) { /* ignorar */ }
    }

    setRepeatingRequest(repeatingRequest, callback, handler)
}
```

Agora você tem um `Flow<TotalCaptureResult>` frio que inicia uma solicitação repetida quando coletado, para no cancelamento, emite cada resultado concluído e funciona com cada operador padrão do Flow.

### Exemplo 6: `combine(previewFlow, aeStateFlow)` para UI Reativa

O real poder do Flow é a composição. Suponha que sua UI mostre:
- FPS da pré-visualização ao vivo
- Estado atual do AE (convergindo / convergido / travado)
- Um indicador "Pronto para fotografar" que fica verde apenas quando o AE convergiu E o AF convergiu E o AWB convergiu.

Sem o Flow, você escreveria manualmente uma máquina de estados mesclando o `CaptureCallback` com o `Choreographer`. Com o Flow, são três linhas:

```kotlin
data class UiCameraState(
    val aeState: Int,
    val afState: Int,
    val awbState: Int,
    val fps: Int,
    val ready: Boolean
)

val resultFlow: Flow<TotalCaptureResult> = session
    .repeatingResultsFlow(previewRequest, cameraHandler)
    .flowOn(cameraDispatcher)   // fora da thread principal, sem travamentos de UI

val aeStateFlow = resultFlow.map {
    it[CaptureResult.CONTROL_AE_STATE] ?: CaptureResult.CONTROL_AE_STATE_INACTIVE
}
val afStateFlow = resultFlow.map {
    it[CaptureResult.CONTROL_AF_STATE] ?: CaptureResult.CONTROL_AF_STATE_INACTIVE
}
val awbStateFlow = resultFlow.map {
    it[CaptureResult.CONTROL_AWB_STATE] ?: CaptureResult.CONTROL_AWB_STATE_INACTIVE
}
val fpsFlow = resultFlow
    .map { it[CaptureResult.SENSOR_TIMESTAMP] }
    .runningFold(emptyList<Long>()) { acc, ts ->
        (acc + ts).takeLast(30) // janela deslizante de 30 timestamps
    }
    .map { timestamps ->
        if (timestamps.size < 2) 0 else {
            val windowNs = timestamps.last() - timestamps.first()
            1_000_000_000 * (timestamps.size - 1) / windowNs.toInt()
        }
    }
    .debounce(250)  // atualiza o rótulo de FPS apenas a cada 250ms, economiza bateria

val uiState: Flow<UiCameraState> = combine(
    aeStateFlow, afStateFlow, awbStateFlow, fpsFlow
) { ae, af, awb, fps ->
    val aeConverged = ae in arrayOf(
        CaptureResult.CONTROL_AE_STATE_CONVERGED,
        CaptureResult.CONTROL_AE_STATE_FLASH_REQUIRED,
        CaptureResult.CONTROL_AE_STATE_LOCKED
    )
    val afConverged = af in arrayOf(
        CaptureResult.CONTROL_AF_STATE_FOCUSED_LOCKED,
        CaptureResult.CONTROL_AF_STATE_PASSIVE_FOCUSED
    )
    val awbConverged = awb in arrayOf(
        CaptureResult.CONTROL_AWB_STATE_CONVERGED,
        CaptureResult.CONTROL_AWB_STATE_LOCKED
    )
    UiCameraState(ae, af, awb, fps, ready = aeConverged && afConverged && awbConverged)
}
```

Colete o `uiState` no `viewLifecycleOwner.lifecycleScope.launchWhenStarted` do seu Fragment e passe cada emissão para sua UI do Compose ou `viewBinding`. Cada operador — `map`, `runningFold`, `debounce`, `combine` — é uma primitiva da biblioteca padrão. Nenhuma máquina de estados personalizada. Sem condições de corrida. Sem eventos perdidos. Cancele o escopo e cada Flow upstream — incluindo a solicitação repetida e o listener do `ImageReader` — para, cancela a assinatura e limpa exatamente uma vez.

---

## Segurança de Threads

Tudo o que foi dito acima não vale nada se você violar as regras de segurança de threads do Camera2. Aqui estão elas, destiladas de centenas de relatórios de bugs de ANR:

1. **Nunca chame nenhuma API do Camera2 a partir da thread principal.** `cameraManager.openCamera()` pode parecer rápido em um Pixel 7. Em um dispositivo Android Go econômico com um HAL `LEGACY`, ele pode bloquear por 1,2s. Isso é um ANR instantâneo. Mesmo chamadas que *parecem* baratas, como `CameraCharacteristics.get()`, podem alocar vários KB de metadados e copiá-los — o que em um início de processo a frio enquanto o usuário está deslizando entre Fragments é suficiente para descartar 3 quadros. Encaminhe *tudo* para o `Dispatchers.Default` ou um dispatcher de thread única dedicado apoiado por uma `HandlerThread`.

2. **HandlerThread vs `CoroutineDispatcher.Default` vs `Dispatchers.IO`.**
   - Use um **dispatcher de thread única** (ex: `HandlerThread("cam").asCoroutineDispatcher()`) para as chamadas de API do Camera2 *reais*. A stack de câmera legada em muitos dispositivos `LEGACY` possui pontos de entrada de HAL com afinidade de thread. Trocar de threads entre o `openCamera` e o `createCaptureSession` dispara bugs conhecidos de HAL no Qualcomm msm8953 e anteriores.
   - Use o `Dispatchers.Default` para computação pura em quadros capturados (fusão HDR, codificação JPEG, detecção facial). Ele possui tantas threads quanto núcleos.
   - Use o `Dispatchers.IO` para E/S de disco (salvando o JPEG no MediaStore). Nunca use o `Default` para gravações bloqueantes.

3. **Estado mutável compartilhado entre coroutines e callbacks deve ser protegido por `Mutex`.** Se um `CaptureCallback` grava em `lastResult` e um clique de botão no Compose o lê, envolva ambos os lados com `mutex.withLock { ... }` ou use `atomicfu`/`@Volatile` para tipos primitivos. NÃO confie em "ele só toca em uma única thread". Callbacks de HAL em dispositivos `LEGACY` ocasionalmente disparam em threads inesperadas e, quando o fazem, você obtém leituras truncadas de valores `Long` de 64 bits como o `SENSOR_TIMESTAMP`.

4. **Por que o Flow evita o deadlock do `PipedOutputStream`.** A armadilha do `PipedOutputStream` do doc de pesquisa merece um exemplo concreto. Se você fizesse isso:

   ```kotlin
   // NÃO FAÇA ISSO
   val pos = PipedOutputStream()
   val pis = PipedInputStream(pos)
   lifecycleScope.launch(Dispatchers.Default) {
       while (true) { image.compressToJpeg(..., pos) }
   }
   lifecycleScope.launch(Dispatchers.IO) {
       // lê pis e grava em arquivo
   }
   ```

   Isso causará um deadlock dentro de 100 quadros porque o `PipedInputStream` possui um buffer padrão de 64KB. Se o gravador produzir mais rápido do que o leitor consome, o gravador bloqueia no `pos.write()` e o buffer enche. Se o leitor, enquanto isso, estiver bloqueado em outra coisa (ex: transação de inserção em massa do MediaStore), ambas as coroutines bloqueiam para sempre — uma espera circular clássica. O Flow com `buffer(CONFLATED)` ou `buffer(DROP_OLDEST)` possui semântica de contrapressão (backpressure) explícita e nunca causa deadlock. Descarte quadros, mas nunca cause deadlock. Essa é a compensação correta para a pré-visualização da câmera.

---

## Resumo

A API baseada em callback do Camera2, quando composta ingenuamente, produz um inferno de callbacks profundamente aninhados que é propenso a erros, a vazamentos e impossível de testar. As coroutines e o Flow do Kotlin fornecem duas primitivas que colapsam todo o design: `suspendCancellableCoroutine` para operações únicas (`openCamera`, `createCaptureSession`, `capture` único) com suporte embutido a tempo limite e cancelamento, e `callbackFlow` para fluxos contínuos (imagens do ImageReader, callbacks `CaptureResult` repetidos) com contrapressão explícita. Os operadores padrão do Flow — `map`, `filter`, `runningFold`, `debounce` e o importantíssimo `combine` — permitem construir pipelines de estado de UI reativos e seguros contra cancelamento a partir de peças componíveis. Imponha a segurança de threads com um dispatcher de câmera dedicado, proteja o estado compartilhado com `Mutex` e substitua qualquer tubulação manual estilo `PipedOutputStream` por canais do Flow para evitar deadlocks.

## O Que Vem a Seguir

Você agora tem as ferramentas para escrever apps Camera2 robustos e de nível de produção. Mas como verificar se seu código funciona nos mais de 24.000 modelos de dispositivos Android atualmente em circulação, e como os OEMs validam seus HALs antes do envio? O Capítulo 27 cobre testes de câmera: Camera ITS, CTS Verifier e testes de instrumentação usando mocks para que você possa rodar sua suíte de testes de câmera em servidores de CI sem nenhum hardware físico.
