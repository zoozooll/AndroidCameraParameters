---
sidebar_position: 4
title: "Capítulo 4: Explorando a Câmera do Seu Celular"
description: Use o aplicativo Android Camera Parameters para explorar as capacidades da câmera do seu dispositivo, incluindo IDs de câmera, resoluções, FPS, nível de hardware, suporte a RAW e muito mais.
keywords: [parâmetros de câmera, IDs de câmera, nível de hardware, suporte a RAW, FPS, resolução]
---

É aqui que o aprendizado se torna interativo. Vamos explorar a câmera do seu celular!

## Introdução

Ler sobre câmeras é útil, mas nada supera ver dados reais do seu próprio dispositivo. É aí que entra o Android Camera Parameters.

Este capítulo é todo sobre **exploração**. Ainda não há programação. Apenas curiosidade.

## Instale o Android Camera Parameters

Se ainda não o fez, instale o aplicativo Android Camera Parameters no seu dispositivo Android:

1. Abra a Google Play Store
2. Pesquise por "Android Camera Parameters"
3. Instale o aplicativo
4. Abra-o e conceda as permissões de câmera

## Entendendo a Interface

Quando você abrir o aplicativo, verá várias seções:

### Tela de Visão Geral
Mostra informações básicas da câmera rapidamente:
- IDs de câmera
- Orientação da lente
- Nível de hardware
- Tamanho do sensor
- Capacidades disponíveis

### Tela de Categorias
Organiza os parâmetros da câmera em grupos lógicos:
- Informações da câmera
- Sensor
- Lente
- Controle
- Scaler
- Flash
- E mais

### Tela de RAW JSON
Exibe o CameraCharacteristics completo em formato JSON bruto para usuários avançados.

## Vamos Explorar

Vamos analisar as principais informações que você deve procurar.

### IDs de Câmera

O Android atribui a cada câmera um número de ID único. Procure por:
- **Câmera 0** — Geralmente a câmera traseira grande angular
- **Câmera 1** — Pode ser a câmera frontal ou outra câmera traseira
- **Câmera 2** — Frequentemente a câmera ultra-angular ou telefoto
- **Câmera 3+** — Câmeras adicionais (macro, profundidade, etc.)

Cada ID representa um dispositivo de câmera separado com suas próprias características.

### Nível de Hardware

Esta é uma das informações mais importantes:

| Nível | Descrição |
| --- | --- |
| **LEGACY** | Dispositivos antigos, suporte limitado ao Camera2 |
| **LIMITED** | Recursos básicos do Camera2 |
| **FULL** | Controles manuais completos, suporte a RAW |
| **LEVEL_3** | Recursos avançados como reprocessamento YUV |

Verifique qual nível de hardware seu celular suporta. Isso determina quais recursos do Camera2 estão disponíveis.

### Informações do Sensor

Procure por:
- **Tamanho do sensor** — Dimensões físicas do sensor
- **Tamanho do array ativo** — A área real usada para capturar imagens
- **Tamanho do array de pixels** — Total de pixels no sensor
- **Duração máxima do quadro** — Tempo mínimo entre quadros
- **Formatos de saída** — JPEG, RAW, YUV, etc.

### Opções de Resolução

As câmeras suportam múltiplas resoluções. Verifique:
- **Tamanhos de pré-visualização** — Resoluções disponíveis para exibição
- **Tamanhos de foto** — Resoluções disponíveis para captura de fotos
- **Tamanhos de vídeo** — Resoluções disponíveis para gravação de vídeo

Observe as diferentes proporções de tela: 4:3, 16:9, 1:1.

### Taxa de Quadros (FPS)

Procure por:
- **Faixa de FPS de pré-visualização** — Quadros por segundo para a pré-visualização
- **Faixa de FPS de captura** — Quadros por segundo para captura de imagem estática
- **Vídeo de alta velocidade** — Modos especiais de alta taxa de quadros

Maior FPS significa vídeo mais suave e foco automático mais responsivo.

### Suporte a RAW

Verifique se sua câmera suporta captura RAW:
- **Formatos RAW** — RAW_SENSOR, RAW10, RAW12, RAW16
- **Tamanhos RAW** — Resoluções disponíveis para captura RAW

O suporte a RAW requer pelo menos nível de hardware FULL.

### Capacidades do Flash

Procure por:
- **Modo de flash** — OFF, ON, AUTO, TORCH
- **Modos disponíveis** — Quais recursos de flash são suportados
- **Informações do flash** — Intensidade e capacidades do flash

### Zoom

Verifique:
- **Zoom digital máximo** — Quanto você pode ampliar digitalmente
- **Distâncias focais disponíveis** — Diferentes lentes e suas distâncias focais
- **Suporte a zoom suave** — Se o zoom pode ser ajustado suavemente

### Modos de Foco

Procure por:
- **Modos de foco disponíveis** — AUTO, FIXED, MACRO, CONTINUOUS_VIDEO, CONTINUOUS_PICTURE, EDGE
- **Faixa de distância de foco** — Distância mínima e máxima de foco
- **Regiões AF** — Número de regiões de foco automático suportadas

### Controle de Exposição

Verifique:
- **Modos AE** — AUTO, ON, OFF
- **Modos AE disponíveis** — Quais modos de exposição são suportados
- **Faixa de exposição** — Tempos de exposição mínimo e máximo
- **Faixa ISO** — Valores ISO suportados

## Sua Vez

Agora é sua vez de explorar. Responda a estas perguntas sobre o seu celular:

1. Quantas câmeras seu celular tem?
2. Qual nível de hardware elas suportam?
3. Qual câmera suporta RAW?
4. Qual é a maior resolução disponível?
5. Alguma câmera suporta vídeo 4K?
6. Qual é o nível máximo de zoom?
7. Seu celular tem câmera telefoto ou ultra-angular?

## Por Que Isso Importa

Você pode se perguntar por que estamos explorando antes de programar. Aqui está o porquê:

1. **Cada celular é diferente** — O que funciona em um dispositivo pode não funcionar em outro
2. **Camera2 requer adaptação** — Bons aplicativos Camera2 consultam as capacidades, não as assumem
3. **Entender cria intuição** — Quando você vê dados reais, conceitos abstratos se tornam concretos

Quando começarmos a programar, você já saberá o que esperar do seu dispositivo.

## Compare com Amigos

Se você tiver amigos com celulares diferentes, compare suas descobertas:
- O celular flagship tem melhor nível de hardware?
- Celulares de entrada não têm suporte a RAW?
- Como a contagem de câmeras varia?

Isso ajuda você a entender o ecossistema de câmeras Android.

## Descobertas Comuns

Aqui estão algumas coisas comuns que as pessoas descobrem:

- **Celulares flagship** frequentemente têm nível de hardware FULL ou LEVEL_3
- **Celulares de entrada** frequentemente têm nível de hardware LIMITED ou LEGACY
- **A maioria dos celulares** suporta captura JPEG
- **Suporte a RAW** ainda não é universal
- **Múltiplas câmeras** são padrão em celulares modernos
- **Câmeras frontais** geralmente têm menor resolução que as câmeras traseiras

## Próximo Capítulo

Agora que você explorou a câmera do seu celular, está pronto para começar a programar! No próximo capítulo, apresentaremos a primeira classe do Camera2: **CameraManager**.

CameraManager é o ponto de entrada para a API Camera2. Ele permite que você:
- Enumere as câmeras disponíveis
- Obtenha as características da câmera
- Abra câmeras

Vamos começar!

## Resumo

Explorar a câmera do seu celular é a melhor maneira de entender o que o Camera2 pode fazer. O Android Camera Parameters torna isso fácil exibindo todas as capacidades da câmera de forma organizada.

Principais coisas a procurar:
- IDs de câmera e suas funções
- Nível de hardware (LEGACY, LIMITED, FULL, LEVEL_3)
- Opções de resolução
- Suporte a RAW
- Capacidades de flash e zoom
- Controles de foco e exposição

Esta exploração prática constrói a base para escrever aplicativos Camera2.
