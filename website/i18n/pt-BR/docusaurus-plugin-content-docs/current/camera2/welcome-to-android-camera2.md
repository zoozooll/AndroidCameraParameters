---
sidebar_position: 1
title: "Capítulo 1: Bem-vindo ao Android Camera2"
description: Saiba por que o Android Camera2 é importante, como ele difere do CameraX e o que esta série abordará.
keywords: [Android Camera2, CameraX, recursos da câmera, desenvolvimento de câmera Android]
---

Antes de controlar a câmera, precisamos entender o sistema da câmera.

## Introdução

Quase todos os smartphones atuais possuem um sistema de câmera potente. Um telefone moderno pode:

- Capturar fotos com aparência profissional
- Gravar vídeos em 4K e 8K
- Criar efeitos de retrato
- Fotografar em luz extremamente baixa
- Capturar vídeos em câmera lenta
- Gerar informações de profundidade
- Combinar várias câmeras juntas

Mas quando você abre o aplicativo de câmera padrão, vê apenas uma interface simples: um botão de obturador, um controle de zoom e alguns modos de disparo.

Por trás dessa interface simples existe um sistema surpreendentemente complexo. O aplicativo de câmera se comunica com componentes de hardware, processadores de imagem e frameworks Android para produzir cada quadro.

Como desenvolvedores Android, podemos querer criar aplicativos que vão além do aplicativo de câmera padrão, como:

- Um aplicativo de fotografia manual
- Uma ferramenta de teste de câmera
- Um aplicativo de visão computacional
- Um aplicativo de digitalização 3D
- Um gravador de vídeo profissional
- Um analisador de capacidade de câmera

Para construir esses aplicativos, precisamos entender a API Android Camera2.

## O que é Android Camera2?

O Android Camera2 é o framework de câmera moderno introduzido pelo Google no Android 5.0 (API nível 21). Ele substituiu a API original da câmera Android.

A antiga API da câmera foi projetada para um mundo mais simples: uma câmera, captura básica de fotos e gravação de vídeo simples. As câmeras dos smartphones evoluíram dramaticamente desde então. Os dispositivos modernos podem conter várias câmeras traseiras, lentes grande angular, lentes telefoto, sensores de profundidade e câmeras externas.

Eles também suportam recursos avançados:

- Exposição e foco manuais
- Captura de imagem RAW
- Vídeo de alta velocidade
- Processamento HDR
- Estabilização óptica

O Camera2 foi criado para dar aos desenvolvedores um controle muito mais profundo sobre o hardware da câmera.

## Camera2 vs. CameraX

O Camera2 e o CameraX resolvem problemas diferentes.

### CameraX

O CameraX é uma biblioteca de nível superior projetada para facilitar tarefas comuns de câmera, incluindo:

- Exibição de uma visualização
- Tirar fotos
- Gravação de vídeos
- Lidar com a compatibilidade do dispositivo

A maioria dos aplicativos deve começar com o CameraX.

### Camera2

O Camera2 é o framework de nível inferior. Ele dá aos desenvolvedores acesso direto aos recursos da câmera, incluindo informações do sensor, configurações de exposição, controles de foco, metadados da câmera, recursos de hardware e suporte RAW.

O Camera2 é mais complexo, mas oferece muito mais controle.

| | CameraX | Camera2 |
| --- | --- | --- |
| Nível | Biblioteca de nível superior | API de nível inferior |
| Dificuldade | Mais fácil | Mais complexo |
| Controle | Limitado | Extenso |
| Melhor para | Aplicativos de câmera normais | Aplicativos de câmera avançados |

Esta série de tutoriais concentra-se no Camera2 porque entendê-lo nos ajuda a compreender como as câmeras Android realmente funcionam.

## Por que aprender Camera2?

Você pode perguntar: *Por que devo aprender Camera2 se o CameraX já existe?*

### Entenda o que o dispositivo realmente pode fazer

Cada telefone Android é diferente. Um dispositivo pode suportar captura RAW, vídeo 4K a 60 fps e controles manuais; outro pode não suportar. O Camera2 permite que os aplicativos descubram esses recursos.

### Construa aplicativos de câmera profissionais

Aplicativos que precisam de recursos de câmera avançados geralmente precisam do Camera2. Os exemplos incluem aplicativos de câmera profissional, aplicativos de imagens científicas, aplicativos de RA, sistemas de visão computacional e ferramentas de produção de vídeo.

### Entenda a fotografia de smartphones

Muitos recursos de câmeras modernas são baseados em conceitos expostos através do Camera2:

- Exposição
- ISO
- Foco
- Equilíbrio de branco
- HDR
- Múltiplas câmeras

Aprender Camera2 também ensina como funcionam as câmeras dos smartphones.

## O que você aprenderá nesta série?

Esta série foi projetada para levá-lo do iniciante ao avançado.

### Parte 1: Entendendo as câmeras

Você aprenderá como as câmeras dos smartphones funcionam, o que o hardware da câmera contém, como o Android representa as câmeras e como inspecionar seu próprio dispositivo.

### Parte 2: Seu primeiro aplicativo Camera2

Você aprenderá como encontrar e abrir câmeras, criar uma visualização e capturar imagens.

### Parte 3: Controles da câmera

Você aprenderá sobre exposição, ISO, foco, equilíbrio de branco, flash e zoom.

### Parte 4: Recursos avançados da câmera

Você aprenderá sobre captura RAW, vídeo de alta velocidade, dispositivos multicâmera, câmeras lógicas e físicas, extensões de câmera e recursos HDR.

### Parte 5: Mergulho profundo nos metadados da câmera

Você explorará parâmetros importantes do Camera2, incluindo:

- `android.sensor.info.activeArraySize`
- `android.scaler.availableMaxDigitalZoom`
- `android.control.aeAvailableModes`
- `android.request.availableCapabilities`

Você entenderá não apenas o que esses parâmetros significam, mas também por que eles existem.

## Aprendendo com o Android Camera Parameters

Ler a documentação é útil, mas os recursos da câmera são mais fáceis de entender quando você pode ver dados reais de um telefone real. Ao longo desta série, usaremos o [Android Camera Parameters](/) para explorar informações reais da câmera.

Você pode usar o aplicativo para descobrir:

- Câmeras disponíveis
- Resoluções suportadas
- Taxas de quadros
- Informações do sensor
- Suporte para controle manual
- Capacidade RAW
- Nível de hardware

Em vez de aprender com exemplos abstratos, você pode investigar diretamente seu próprio dispositivo.

## Para quem é este tutorial?

Esta série foi projetada para:

- **Desenvolvedores Android** que desejam entender o sistema de câmera além das APIs básicas.
- **Desenvolvedores de aplicativos de câmera** que precisam de recursos avançados de câmera.
- **Desenvolvedores de visão computacional** que precisam de acesso aos quadros e metadados da câmera.
- **Desenvolvedores curiosos** que desejam entender como as câmeras dos smartphones realmente funcionam.

## Antes de começarmos a codificar

O Camera2 não é difícil porque a API foi mal projetada. É difícil porque as câmeras modernas são extremamente potentes.

Uma câmera de smartphone não é mais apenas um sensor que captura imagens. É um sistema de imagem completo envolvendo:

- Hardware
- Firmware
- Processamento ISP
- Framework Android
- Software de aplicação

O Camera2 expõe essa complexidade aos desenvolvedores. Nosso objetivo nesta série é entendê-la passo a passo.

## Próximo capítulo

No próximo capítulo, **Entendendo o Hardware da Câmera do Smartphone**, deixaremos o Android por um momento e exploraremos a própria câmera. Você aprenderá o que um sensor de câmera faz, por que sensores maiores produzem imagens melhores, o que as lentes realmente significam, como o processamento ISP funciona e por que dois telefones com contagens de megapixels semelhantes podem produzir fotos completamente diferentes.

Depois de entender o hardware, os conceitos do Camera2 se tornarão muito mais fáceis.

## Resumo

O Android Camera2 é a base para a construção de aplicativos de câmera avançados no Android. Ele fornece acesso direto aos recursos e controles da câmera que estão ocultos atrás de aplicativos de câmera normais.

Esta série o guiará desde a compreensão das câmeras dos smartphones até a construção de aplicativos Camera2 de nível profissional. Vamos começar a jornada.
