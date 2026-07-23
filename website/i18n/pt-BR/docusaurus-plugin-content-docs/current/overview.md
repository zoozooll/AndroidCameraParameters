---
sidebar_position: 1
slug: /
description: Visão geral do painel do Android Camera Parameters e seus principais recursos de diagnóstico, incluindo detecção de nível de hardware e rastreamento de recursos em tempo real.
keywords: [painel da câmera android, detecção de nível de hardware, diagnóstico de câmera]
---

# Visão Geral do App

Esta página fornece um detalhamento do painel da aplicação e de seus principais recursos.

![Visão Geral do App](/img/camera_params_feature_graph.png)

## Componentes do Painel

### 1. Navegação e Seleção
- **Menu de Navegação**: Acesso rápido às Configurações, Exportar/Importar, alternar Temas e informações Sobre.
- **Seleção de Câmera**: Clique no Nome da Câmera ou no Selo de ID para alternar entre as lentes Traseira, Frontal e Externa.

### 2. Cartão de Resumo
O Cartão de Resumo fornece um instantâneo de alto nível da câmera selecionada:
- **Nível de Hardware**: Indica o nível de suporte da API Camera2 (LEGACY, LIMITED, FULL, LEVEL_3).
- **Resolução do Sensor**: A contagem total de megapixels do sensor.
- **FPS Máximo de Vídeo**: A taxa de quadros mais alta suportada para gravação de vídeo.

### 3. Grade de Principais Recursos
Uma grade de visualização rápida mostrando o suporte para recursos profissionais críticos:
- **Suporte a RAW**: Capacidade de capturar dados do sensor não compactados.
- **Exposição e Foco Manual**: Controle de nível profissional sobre a captura de imagem.
- **Recursos de Flash**: Suporte para Flash Automático e redução de olhos vermelhos.
- **OIS (Estabilização Óptica de Imagem)**: Redução de trepidação baseada em hardware.
- **HDR e Detecção de Rosto**: Recursos inteligentes de processamento de cena.

### 4. Categorias de Parâmetros
As características detalhadas da câmera são agrupadas em categorias lógicas para facilitar a exploração:
- **Sensor**: Tamanho da matriz ativa, sensibilidade, intervalos de tempo de exposição.
- **Lente**: Abertura, distância focal, distância de foco.
- **AE/AF/AWB**: Controles detalhados para Exposição Automática, Foco Automático e Balanço de Branco Automático.
- **Saída**: Tamanhos suportados para JPEG, RAW e YUV.
