---
sidebar_position: 2
description: Arquitetura técnica do aplicativo Android Camera Parameters, incluindo detalhes do padrão MVVM, estrutura de interface Jetpack Compose e diretrizes de desenvolvimento.
keywords: [desenvolvimento android, mvvm, jetpack compose, tutorial api camera2]
---

# Documentação do Desenvolvedor

Este documento fornece uma visão técnica do aplicativo **Android Camera Parameters**, sua arquitetura e diretrizes de desenvolvimento.

## Visão Geral do Projeto

O aplicativo é uma ferramenta de diagnóstico para inspecionar `CameraCharacteristics` da API Camera2 do Android. Ele fornece uma interface moderna e amigável para explorar níveis de hardware, capacidades e valores brutos de parâmetros para todas as lentes de câmera em um dispositivo.

## Arquitetura

O projeto segue o padrão arquitetural **MVVM (Model-View-ViewModel)** e é construído usando **Jetpack Compose** para a camada de interface do usuário (UI).

### Componentes Principais

#### `CameraParamsActivity`
O ponto de entrada único do aplicativo.
- Lida com permissões em tempo de execução (CAMERA).
- Inicializa a interface Compose via `setContent`.
- Hospeda o `CameraParamsTheme`.

#### `CameraViewModel`
O gerenciador de estado central para a UI.
- Mantém o `UiState`, que inclui a lista de câmeras, índice selecionado, parâmetros categorizados e consulta de busca.
- **Detecção de Recursos**: Contém lógica em `detectFeatureFlags()` para determinar dinamicamente capacidades de hardware como suporte RAW, OIS e Exposição Manual.
- **Categorização**: Agrupa centenas de chaves da Camera2 em seções lógicas (Sensor, Lente, etc.) para melhor legibilidade.

#### `CameraParamsHelper`
Um wrapper utilitário em torno do `CameraManager` do Android.
- Recupera `CameraCharacteristics` para IDs específicos.
- Fornece formatação especializada para tipos de câmera complexos (ex: convertendo modos `IntArray` em strings legíveis por humanos).

## Camada de UI (Jetpack Compose)

A interface é construída usando **Material 3** com um tema escuro estritamente aplicado.

### Estrutura de Navegação

O aplicativo usa `androidx.navigation.compose` gerenciado em `MainScreen.kt`.

| Tela | Responsabilidade |
| :--- | :--- |
| **[Visão Geral](overview.md)** | Painel de alto nível mostrando o Cartão de Resumo, Nível de Hardware e chips de Principais Recursos. |
| **Categorias** | Lista expansível de todos os parâmetros agrupados por seção com filtragem de busca. |
| **Bruto (JSON)** | Representação JSON com destaque de sintaxe de todas as propriedades da câmera. |
| **Detalhe** | Visualização focada para um único parâmetro, mostrando o valor formatado e dados brutos. |

### Estilização

- **Tema**: Definido em `Theme.kt`.
- **Cores**: Cor primária `#7B61FF` (Violeta) usada para destaques e ações primárias.
- **Superfície**: Fundo escuro `#121417` com variantes `#1E1F23` para cartões.

## Lógica Principal

### Detecção Dinâmica de Recursos

Os chips de "Principais Recursos" no painel não são estáticos. Eles são computados em `CameraViewModel.detectFeatureFlags()`:

- **RAW**: Verificado via `REQUEST_AVAILABLE_CAPABILITIES_RAW`.
- **OIS**: Detectado se `LENS_INFO_AVAILABLE_OPTICAL_STABILIZATION` contiver `ON`.
- **Exposição Manual**: Disponível se `CONTROL_AE_MODE_OFF` for suportado.
- **Foco Manual**: Habilitado se `LENS_INFO_MINIMUM_FOCUS_DISTANCE` for maior que 0.

## Guia de Desenvolvimento

### Pré-requisitos
- Android Studio Ladybug (ou mais recente).
- Kotlin 2.0+ (O projeto usa o novo plugin Gradle do Compose Compiler).
- SDK Mínimo: 21 (Android 5.0).

### Adicionando uma Nova Categoria
Para adicionar ou modificar o agrupamento de parâmetros, atualize o método `getCategoryForKey()` em `CameraViewModel.kt`. Ele usa correspondência de strings nos nomes das chaves da câmera para atribuí-las a categorias.

### Atualizando o Tema
As cores podem ser ajustadas em `Color.kt`. O aplicativo foi projetado para ter a melhor aparência no modo escuro; quaisquer alterações na paleta clara devem ser testadas cuidadosamente.
