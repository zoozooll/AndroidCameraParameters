---
sidebar_position: 1
slug: /
description: Visão geral do painel do Android Camera Parameters e seus principais recursos de diagnóstico, incluindo detecção de nível de hardware e rastreamento de recursos em tempo real.
keywords: [painel da câmera android, detecção de nível de hardware, diagnóstico de câmera]
---

# Visão Geral do Aplicativo

Esta página fornece um detalhamento do painel do aplicativo e de seus principais recursos.

![Visão Geral do Aplicativo](/img/camera_params_feature_graph.png)

## Componentes do Painel

### 1. Navegação e Seleção
- **Menu Lateral**: Acesse a Política de Privacidade, Avalie o Aplicativo e informações Sobre através do ícone de menu no canto superior esquerdo.
- **Seleção de Câmera**: Toque no Nome da Câmera ou no Crachá de ID (ex: "0") para abrir um menu suspenso e alternar entre as lentes disponíveis (Traseira, Frontal, Ultra-wide, etc.).
- **Navegação Inferior**: Alterne facilmente entre **Visão Geral**, **Categorias**, **JSON Bruto** e **Favoritos**.

### 2. Cartão de Resumo
O Cartão de Resumo no topo fornece a informação mais crítica:
- **Nível de Hardware**: O nível de suporte da API Camera2 (LEGACY, LIMITED, FULL ou LEVEL_3). Isso determina as capacidades gerais da lente.

### 3. Grade de Principais Recursos
Uma grade visual que fornece o status instantâneo para recursos de nível profissional:
- **Resolução e Tamanho do Sensor**: Características físicas do sensor.
- **FPS Máximo de Vídeo**: Capacidades de taxa de quadros de pico.
- **Suporte RAW**: Indica se o sensor pode gerar dados não compactados.
- **OIS (Estabilização Óptica de Imagem)**: Disponibilidade de estabilização física da lente.
- **Controle Manual**: Status do suporte para Exposição Manual e Foco Manual.
- **Processamento**: Suporte para HDR, Detecção de Rosto e Redução de Olhos Vermelhos.

### 4. Parâmetros Categorizados (Aba Categorias)
Explore a lista completa de CameraCharacteristics organizada em grupos lógicos:
- **Sensor**: Resolução, tamanho físico, faixas de sensibilidade.
- **Lente**: Distância focal, abertura, modos de estabilização.
- **AE/AF/AWB**: Modos de controle detalhados para exposição, foco e balanço de branco.
- **Busca**: Use a barra de busca integrada para encontrar rapidamente chaves ou valores específicos da API.
