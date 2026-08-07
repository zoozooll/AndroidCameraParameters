---
sidebar_position: 26
title: "Chapitre 26 : Programmation asynchrone de la caméra"
description: "Apprivoisez l'enfer des rappels (callback hell) de Camera2 en utilisant les coroutines Kotlin et Flow. Apprenez suspendCancellableCoroutine pour les opérations ponctuelles (openCamera, createCaptureSession, capture), callbackFlow pour les flux continus d'ImageReader et de CaptureResult, les opérateurs de combinaison pour les interfaces réactives, et les modèles de sécurité des threads pour prévenir les ANR et les blocages."
keywords: [coroutines kotlin camera2, callback hell, suspendcancellablecoroutine, callbackflow, camera2 flow, sécurité threads camera2, mutext état partagé, blocage pipedoutputstream, interface caméra réactive]
---

# Chapitre 26 : Programmation asynchrone de la caméra

## Résumé

Revenez en arrière et regardez le code que vous avez écrit pour les chapitres 7 à 9. `CameraDevice.StateCallback` imbriqué dans `CameraManager.openCamera`, avec `CameraCaptureSession.StateCallback` imbriqué dans `onOpened`, avec `CaptureCallback` imbriqué dans `onConfigured`, avec `ImageReader.OnImageAvailableListener` se déclenchant sur un `HandlerThread` que vous avez lancé à la main et que vous devez démonter exactement dans l'ordre inverse sur chaque chemin d'erreur. C'est l'enfer des rappels, version caméra. Chaque niveau d'indentation est une nouvelle classe de rappel. Chaque erreur doit se propager à travers quatre couches d'objets anonymes. Chaque `close()` oublié sur le chemin du démontage fait fuiter la caméra jusqu'au redémarrage.

Ce chapitre est la refonte que vous attendiez. Nous convertissons toute la jungle des rappels en un code Kotlin propre, linéaire, annulable et testable en utilisant deux primitives de coroutines : `suspendCancellableCoroutine` pour les opérations ponctuelles, et `callbackFlow` + les opérateurs `Flow` pour les flux continus. Vous apprendrez les règles de sécurité des threads pour les coroutines interagissant avec Camera2, pourquoi bloquer le thread principal sur n'importe quel appel de caméra est un ANR garanti, et pourquoi le modèle `PipedOutputStream`/`PipedInputStream` que vous avez peut-être essayé pour les données `ImageWriter` produit des blocages que `Flow` évite naturellement.

Comme toujours, validez les capacités de niveau matériel que vous ciblez avec **Android Camera Parameters** ([Google Play](https://play.google.com/store/apps/details?id=com.minininja.cameraparams), [GitHub](https://github.com/zoozooll/AndroidCameraParameters)) pour confirmer que les capacités dont votre pipeline asynchrone a besoin (rafale répétée, résultats partiels, retraitement YUV) sont réellement présentes sur vos appareils de test.

---

## Pourquoi les rappels imbriqués sont un "enfer des rappels"

Visualisons d'abord le problème. Voici une structure réelle (simplifiée) provenant d'une application Camera2 brute en production avant les coroutines :

```mermaid
graph TD
    A["onCreateView"] -->|cameraId choisi| B["CameraManager.openCamera"]
    B -->|déclenche sur| C[StateCallback.onOpened<br/>lambda 1]
    C -->|détient cameraDevice| D[createCaptureSession<br/>(sorties = previewSurface + imageReaderSurface)]
    D -->|déclenche sur| E[Session.StateCallback.onConfigured<br/>lambda 2]
    E -->|détient session| F[session.setRepeatingRequest<br/>+ CaptureCallback lambda 3]
    F -->|déclenche onProgress| G[CaptureCallback.onCaptureProgressed<br/>résultats partiels]
    F -->|déclenche onCompleted| H[CaptureCallback.onCaptureCompleted<br/>TotalCaptureResult]
    H -->|image prête| I[ImageReader.OnImageAvailableListener<br/>lambda 4]
    I -->|octets JPEG| J[Appel enregistrement MediaStore<br/>lambda 5]

    style A fill:#e6d9f2
    style C fill:#f2d9e6
    style E fill:#f2d9e6
    style H fill:#f2d9e6
    style I fill:#f2d9e6
    style J fill:#d9f2e6
```

Chaque rappel ombré est une classe anonyme distincte. Chacun capture une référence à des ressources situées deux niveaux plus haut. Chaque chemin d'erreur doit remonter de J vers A, fermant `imageReader → session → cameraDevice → handlerThread` dans l'ordre inverse, et toute omission d'un `close()` dans l'une des 16 permutations d'erreurs produit une fuite permanente de la caméra jusqu'au redémarrage de l'appareil. C'est la définition même de l'enfer des rappels.

L'objectif de ce chapitre est de transformer ces spaghettis en ceci :

```mermaid
flowchart LR
    A["openCameraAwait()"] --> B["createCaptureSessionAwait()"]
    B --> C[setRepeatingRequestFlow()]
    C --> D[aeStateFlow.map().combine(previewFlow)]
    D --> E[État UI<br/>(émission unique)]
    style A fill:#d9f2e6
    style B fill:#d9f2e6
    style C fill:#d9f2e6
    style D fill:#d9f2e6
    style E fill:#e6d9f2
```

Linéaire. Composable. Testable. Annulable par l'annulation du `Job` parent. Chaque étape est une fonction simple ou un opérateur `Flow`. Les cinq mêmes rappels vivent désormais dans un pipeline linéaire de 12 lignes.

---

## Coroutines Kotlin pour les opérations ponctuelles : `suspendCancellableCoroutine`

Le modèle de base pour envelopper toute API basée sur des rappels en tant que fonction `suspend` est `suspendCancellableCoroutine`. La recette est toujours identique :

1. Appeler `suspendCancellableCoroutine { cont -> ... }` pour obtenir une `CancellableContinuation<T>`.
2. Appeler l'API réelle basée sur des rappels, en lui passant une implémentation de rappel anonyme.
3. Dans le chemin de succès du rappel, appeler `cont.resume(valeur)`.
4. Dans chaque chemin d'erreur, appeler `cont.resumeWithException(t)`.
5. Dans `cont.invokeOnCancellation { ... }`, effectuer le nettoyage : fermer la caméra, annuler les requêtes en attente, désinscrire les écouteurs pour que le rappel ne se déclenche jamais *après* l'annulation de la coroutine.
6. Envelopper le tout dans un `withTimeout` aux sites d'appel afin qu'un HAL bloqué ne puisse pas figer votre application indéfiniment.

### Exemple 1 : `suspend fun openCameraAwait()`

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
                    "Caméra $cameraId déconnectée pendant l'ouverture"
                )
            )
        }
        override fun onError(camera: CameraDevice, error: Int) {
            cont.resumeWithException(
                CameraAccessException(error, "Erreur caméra $cameraId : $error")
            )
        }
    }

    cont.invokeOnCancellation {
        try {
            // Solution de contournement : openCamera() n'expose pas de handle annulable
            // avant l'API 30. Fermer l'appareil s'il a été ouvert dans la fenêtre de course.
        } catch (_: Throwable) { /* ignorer */ }
    }

    openCamera(cameraId, callback, handler)
}
```

**Pourquoi cela fonctionne.** `openCamera` est de type "déclencher et oublier" : vous l'appelez, et à un moment futur, l'une des trois méthodes de rappel se déclenche exactement une fois. Ce contrat ("se déclenche exactement une fois") est ce qui nous permet de le mapper un pour un sur une continuation. Si la coroutine est annulée *avant* qu'un rappel ne se déclenche, `invokeOnCancellation` s'exécute et empêche une fuite de ressource. Si elle est annulée *après* le `resume`, le bloc `resume(valeur) { camera.close() }` — le paramètre `onCancellation` de `resume` — ferme automatiquement l'appareil.

L'appeler avec un timeout est trivial :

```kotlin
val cameraDevice: CameraDevice = withTimeoutOrNull(5_000L) {
    cameraManager.openCameraAwait(cameraId, cameraHandler)
} ?: run {
    Log.w(TAG, "L'ouverture de la caméra a expiré après 5s")
    return@launch
}
```

Si le HAL est bloqué (courant sur les appareils `LEGACY` bas de gamme après une fuite de caméra d'une application précédente), cela échoue rapidement et proprement au lieu de présenter à l'utilisateur une boîte de dialogue "L'application ne répond pas".

### Exemple 2 : `suspend fun createCaptureSessionAwait()`

Même modèle, rappel différent :

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
                    "Échec de la configuration de la session pour l'appareil ${this@createCaptureSessionAwait.id}"
                )
            )
        }
    }

    cont.invokeOnCancellation {
        // Impossible d'annuler la création de session en cours sur les anciennes API.
        // La session se fermera si elle finit par aboutir via le bloc de reprise
        // onCancellation ci-dessus.
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

C'est exactement la même forme. Les deux versions de l'API (`createCaptureSession(surfaces, callback, handler)` avant S vs `SessionConfiguration` S+) sont gérées dans un seul wrapper. Les appelants n'ont jamais besoin de le savoir.

### Exemple 3 : `suspend fun awaitCaptureResult()` pour une capture unique

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
                    "Capture échouée : raison=${failure.reason} image=${failure.frameNumber}"
                )
            )
        }
    }

    cont.invokeOnCancellation {
        try { abortCaptures() } catch (_: Throwable) { /* ignorer */ }
    }

    try {
        capture(request, callback, handler)
    } catch (t: Throwable) {
        cont.resumeWithException(t)
    }
}
```

C'est la brique de base pour le bracketing manuel multi-images de style Chapitre 19 — enchaînez 7 appels `captureAwait(br[i])` dans une boucle `for` avec `withTimeoutOrNull`, collectez les 7 `TotalCaptureResult`, et vous avez une séquence de bracketing HDR complète avec timeout par image et annulation automatique. Dans le monde des rappels, c'était des centaines de lignes de machine à états. C'est maintenant une boucle `for` de 12 lignes.

---

## Flow pour les flux continus

Les opérations ponctuelles couvrent l'ouverture de la caméra, la création de session et la capture unique. Pour les éléments répétitifs — chaque image d'aperçu, chaque rappel `TotalCaptureResult`, chaque `Image` d'un `ImageReader` — nous voulons un `Flow<T>` afin de pouvoir utiliser `map`, `filter`, `debounce`, `combine` et partager les flux entre les abonnés.

### Exemple 4 : ImageReader → `Flow&lt;Image&gt;` via `callbackFlow`

```kotlin
fun ImageReader.imagesFlow(
    lifecycleScope: CoroutineScope
): Flow<Image> = callbackFlow {
    val listener = ImageReader.OnImageAvailableListener { reader ->
        // acquireLatestImage() ignore les anciennes images si le consommateur est plus lent que
        // la caméra — obligatoire pour éviter de bloquer le HAL.
        val image = reader.acquireLatestImage() ?: return@OnImageAvailableListener
        trySend(image)
    }

    // Doit appeler invokeOnClose en PREMIER pour que l'annulation supprime toujours le listener
    // même si setOnImageAvailableListener lui-même lève une exception.
    awaitClose {
        setOnImageAvailableListener(null, null)
        // Ne PAS fermer l'ImageReader ici — l'appelant possède son cycle de vie.
    }

    val handlerThread = HandlerThread("ImageReaderFlow").apply { start() }
    val handler = Handler(handlerThread.looper)
    setOnImageAvailableListener(listener, handler)
}.buffer(Channel.CONFLATED)
 .onCompletion {
     // Toute image non consommée par les collecteurs en aval est à nous de fermer,
     // car callbackFlow renvoie les échecs après émission.
 }
```

**Choix de conception critiques :**

1. `acquireLatestImage()` plutôt que `acquireNextImage()`. Si votre traitement d'image (inférence ML, détection de visage) prend 40 ms et que la caméra filme à 30 fps (~33 ms par image), vous *allez* prendre du retard. `acquireNextImage` les accumule jusqu'à ce que vous manquiez de tampons gralloc et que la caméra se fige. `acquireLatestImage` saute les anciennes et vous donne l'image la plus fraîche. C'est presque toujours ce que vous voulez pour l'analyse d'image côté aperçu.

2. `buffer(Channel.CONFLATED)`. Un tampon "conflated" ne conserve que la dernière valeur. Combiné avec `acquireLatestImage`, c'est une garantie absolue que vous ne mettez jamais en file d'attente des images obsolètes.

3. `awaitClose { setOnImageAvailableListener(null, null) }`. C'est l'équivalent `callbackFlow` de `cont.invokeOnCancellation`. Annulez la portée de la coroutine (par exemple, lorsque le Fragment passe par `onDestroyView`) et l'écouteur est automatiquement désinscrit et le `HandlerThread` nettoyé. *Aucune* fuite.

### Exemple 5 : CaptureCallback → `Flow&lt;TotalCaptureResult&gt;`

Même modèle :

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
            // Si vous avez besoin des résultats partiels, émettez-les sur un canal séparé
            // ou envoyez une classe scellée (sealed class).
        }
    }

    awaitClose {
        try {
            stopRepeating()
            abortCaptures()
        } catch (_: Throwable) { /* ignorer */ }
    }

    setRepeatingRequest(repeatingRequest, callback, handler)
}
```

Vous avez maintenant un `Flow&lt;TotalCaptureResult&gt;` froid qui démarre une requête répétée lorsqu'il est collecté, l'arrête lors de l'annulation, émet chaque résultat terminé et fonctionne avec chaque opérateur Flow standard.

### Exemple 6 : `combine(previewFlow, aeStateFlow)` pour une UI réactive

La véritable puissance de Flow réside dans la composition. Supposons que votre UI affiche :
- Les FPS de l'aperçu en direct
- L'état AE actuel (en convergence / convergé / verrouillé)
- Un indicateur "Prêt à photographier" qui est vert seulement quand l'AE est convergé ET l'AF est convergé ET l'AWB est convergé.

Sans Flow, vous écrivez manuellement une machine à états fusionnant `CaptureCallback` avec `Choreographer`. Avec Flow, cela prend trois lignes :

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
    .flowOn(cameraDispatcher)   // hors thread principal, pas de saccade UI

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
        (acc + ts).takeLast(30) // fenêtre glissante de 30 horodatages
    }
    .map { timestamps ->
        if (timestamps.size < 2) 0 else {
            val windowNs = timestamps.last() - timestamps.first()
            1_000_000_000 * (timestamps.size - 1) / windowNs.toInt()
        }
    }
    .debounce(250)  // mettre à jour l'étiquette FPS seulement toutes les 250ms, économise la batterie

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

Collectez `uiState` dans le `viewLifecycleOwner.lifecycleScope.launchWhenStarted` de votre Fragment et passez chaque émission à votre UI Compose ou `viewBinding`. Chaque opérateur — `map`, `runningFold`, `debounce`, `combine` — est une primitive de la bibliothèque standard. Pas de machine à états personnalisée. Pas de conditions de concurrence. Pas d'événements manqués. Annulez la portée et chaque Flow en amont — y compris la requête répétée et l'écouteur `ImageReader` — s'arrête, se désabonne et se nettoie exactement une fois.

---

## Sécurité des threads

Tout ce qui précède est inutile si vous violez les règles de sécurité des threads de Camera2. Les voici, distillées à partir de centaines de rapports de bugs ANR :

1. **N'appelez jamais d'API Camera2 depuis le thread principal.** `cameraManager.openCamera()` peut sembler rapide sur un Pixel 7. Sur un appareil Android Go à petit budget avec un HAL `LEGACY`, il peut bloquer pendant 1,2 s. C'est un ANR instantané. Même les appels qui *paraissent* peu coûteux, comme `CameraCharacteristics.get()`, peuvent allouer plusieurs Ko de métadonnées et les copier — ce qui, lors d'un démarrage à froid du processus pendant que l'utilisateur bascule entre les Fragments, suffit à faire perdre 3 images. Répartissez *tout* vers `Dispatchers.Default` ou un dispatcher dédié à un seul thread soutenu par un `HandlerThread`.

2. **HandlerThread vs `CoroutineDispatcher.Default` vs `Dispatchers.IO`.**
   - Utilisez un **dispatcher à thread unique** (ex : `HandlerThread("cam").asCoroutineDispatcher()`) pour les appels *réels* à l'API Camera2. La pile de caméras héritée sur de nombreux appareils `LEGACY` possède des points d'entrée HAL affines au thread. Changer de thread entre `openCamera` et `createCaptureSession` déclenche des bugs de HAL connus sur Qualcomm msm8953 et antérieurs.
   - Utilisez `Dispatchers.Default` pour le calcul pur sur les images capturées (fusion HDR, encodage JPEG, détection de visage). Il possède autant de threads que de cœurs.
   - Utilisez `Dispatchers.IO` pour les E/S disque (enregistrement du JPEG dans MediaStore). N'utilisez jamais `Default` pour les écritures bloquantes.

3. **L'état partagé mutable entre les coroutines et les rappels doit être protégé par un `Mutex`.** Si un `CaptureCallback` écrit `lastResult` et qu'un clic sur un bouton Compose le lit, enveloppez les deux côtés avec `mutex.withLock { ... }` ou utilisez `atomicfu`/`@Volatile` pour les types primitifs. Ne vous fiez PAS au fait que "cela ne touche jamais qu'un seul thread". Les rappels de HAL sur les appareils `LEGACY` se déclenchent occasionnellement sur des threads inattendus, et quand c'est le cas, vous obtenez des lectures tronquées de valeurs `Long` 64 bits comme `SENSOR_TIMESTAMP`.

4. **Pourquoi Flow évite le blocage `PipedOutputStream`.** Le piège du `PipedOutputStream` mentionné dans le doc de recherche mérite un exemple concret. Si vous faisiez ceci :

   ```kotlin
   // NE FAITES PAS CECI
   val pos = PipedOutputStream()
   val pis = PipedInputStream(pos)
   lifecycleScope.launch(Dispatchers.Default) {
       while (true) { image.compressToJpeg(..., pos) }
   }
   lifecycleScope.launch(Dispatchers.IO) {
       // lire pis et écrire dans le fichier
   }
   ```

   Cela se bloque en moins de 100 images car `PipedInputStream` possède un tampon par défaut de 64 Ko. Si le producteur produit plus vite que le consommateur ne consomme, le producteur bloque sur `pos.write()` et le tampon se remplit. Si pendant ce temps le lecteur est bloqué sur autre chose (ex : transaction d'insertion groupée MediaStore), les deux coroutines sont bloquées pour toujours — une attente circulaire classique. Flow avec `buffer(CONFLATED)` ou `buffer(DROP_OLDEST)` possède une sémantique de contre-pression explicite et ne bloque jamais. Perdez des images, mais ne bloquez jamais. C'est le bon compromis pour l'aperçu de la caméra.

---

## Résumé

L'API de Camera2 basée sur des rappels, lorsqu'elle est composée naïvement, produit un enfer de rappels profondément imbriqués, sujet aux erreurs, aux fuites et impossible à tester. Les coroutines Kotlin et Flow vous donnent deux primitives qui simplifient toute la conception : `suspendCancellableCoroutine` pour les opérations ponctuelles (`openCamera`, `createCaptureSession`, `capture` unique) avec support intégré du timeout et de l'annulation, et `callbackFlow` pour les flux continus (images d'ImageReader, rappels `CaptureResult` répétés) avec une contre-pression explicite. Les opérateurs Flow standards — `map`, `filter`, `runningFold`, `debounce` et le très important `combine` — vous permettent de construire des pipelines d'état UI réactifs et sûrs contre l'annulation à partir de pièces composables. Imposez la sécurité des threads avec un dispatcher de caméra dédié, protégez l'état partagé avec un `Mutex`, et remplacez tout piping manuel de type `PipedOutputStream` par des canaux Flow pour éviter les blocages.

## Et ensuite ?

Vous avez maintenant les outils pour écrire des applications Camera2 robustes et de qualité production. Mais comment vérifier que votre code fonctionne sur les plus de 24 000 modèles d'appareils Android actuellement en circulation, et comment les OEM valident-ils leurs HAL avant l'expédition ? Le chapitre 27 traite des tests de caméra : Camera ITS, CTS Verifier et les tests d'instrumentation utilisant des mocks pour que vous puissiez exécuter votre suite de tests de caméra sur des serveurs CI sans aucun matériel physique.
