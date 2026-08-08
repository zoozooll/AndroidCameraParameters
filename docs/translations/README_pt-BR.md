# Parâmetros de Câmera do Android (Android Camera Parameters)

<a href='https://play.google.com/store/apps/details?id=com.minininja.cameraparams&pcampaignid=pcampaignidMKT-Other-global-all-screenshots-pipeline'><img alt='Disponível no Google Play' src='https://play.google.com/intl/en_us/badges/static/images/badges/en_badge_web_generic.png' width="240"/></a>
<a href='https://zoozooll.github.io/AndroidCameraParameters/category/mastering-android-camera2-api'><img alt='Tutoriais' src='https://img.shields.io/badge/Tutoriais-Mastering%20Camera2-blue?style=for-the-badge&logo=gitbook' height="45"/></a>

[English](../../README.md) | [简体中文](README_zh-CN.md) | [繁體中文](README_zh-TW.md) | [Español](README_es.md) | [Português] | [Français](README_fr.md) | [Deutsch](README_de.md) | [Русский](README_ru.md) | [हिन्दी](README_hi.md) | [Bahasa Indonesia](README_in.md) | [日本語](README_ja.md) | [한국어](README_ko.md)

Android Camera Parameters é uma poderosa ferramenta de diagnóstico para desenvolvedores e entusiastas explorarem as profundas capacidades técnicas das câmeras de seus dispositivos. Ela utiliza a API Camera2 do Android para fornecer informações detalhadas sobre cada lente em seu dispositivo.

![Visão Geral do App](../../website/static/img/camera_params_feature_graph.png)

## Principais Recursos

*   **Diagnóstico Detalhado**: Inspecione `CameraCharacteristics` para todas as lentes (Traseira, Frontal, Externa).
*   **Detecção de Nível de Hardware**: Veja instantaneamente se seu dispositivo suporta recursos `LEGACY`, `LIMITED`, `FULL` ou `LEVEL_3`.
*   **Rastreamento de Recursos em Tempo Real**: Verifique o suporte para captura RAW, Estabilização Óptica de Imagem (OIS), Exposição Manual, Foco Manual e muito mais através de um painel intuitivo.
*   **Exploração Categorizada**: Centenas de parâmetros organizados pelas categorias Sensor, Lente, AE/AF/AWB e Processamento com funcionalidade de **busca integrada**.
*   **Favoritos (Em breve)**: Salve parâmetros verificados com frequência para acesso rápido.
*   **Exportação de Dados Brutos**: Visualize o perfil completo da câmera como um JSON estruturado.
*   **Suporte Multi-idioma**: Totalmente localizado em mais de 12 idiomas, incluindo chinês, espanhol, japonês e mais.

## Idiomas Suportados

O aplicativo está localizado para suportar um público global:
- 🇺🇸 Inglês
- 🇨🇳 Chinês (Simplificado)
- 🇹🇼/🇭🇰 Chinês (Tradicional)
- 🇪🇸 Espanhol
- 🇧🇷 Português (Brasil)
- 🇫🇷 Francês
- 🇩🇪 Alemão
- 🇷🇺 Russo
- 🇮🇳 Hindi
- 🇮🇩 Indonésio
- 🇯🇵 Japonês
- 🇰🇷 Coreano

## Stack Tecnológica

- **Linguagem**: Kotlin
- **Framework de UI**: Jetpack Compose
- **Sistema de Design**: Material 3
- **Arquitetura**: MVVM
- **Bibliotecas**:
    - [Camera2 API](https://developer.android.com/training/camera2): Interação principal com a câmera.
    - [Gson](https://github.com/google/gson): Serialização JSON para exportação de dados brutos.
    - [Navigation Compose](https://developer.android.com/jetpack/compose/navigation): Navegação no app.

## Estrutura do Projeto

- `app/`: Módulo principal do aplicativo Android.
    - `com.aaron.cameraparams.ui`: Telas e componentes de UI baseados em Compose.
    - `com.aaron.cameraparams.camera`: Lógica para interagir com o CameraManager e recuperar características.
- `camera_parameters/`: Amostras de dumps JSON de parâmetros de câmera de vários dispositivos (Pixel 3, Samsung S10+, etc.).
- `docs/`: Documentação adicional e capturas de tela.

## Documentação

A documentação abrangente está disponível em [https://zoozooll.github.io/AndroidCameraParameters/](https://zoozooll.github.io/AndroidCameraParameters/).

*   **[Tutoriais: Dominando a API Camera2 do Android](https://zoozooll.github.io/AndroidCameraParameters/category/mastering-android-camera2-api)** 📚
*   [Visão Geral do App](https://zoozooll.github.io/AndroidCameraParameters/)
*   [Guia do Desenvolvedor](https://zoozooll.github.io/AndroidCameraParameters/development)
*   [Segurança de Dados](https://zoozooll.github.io/AndroidCameraParameters/data-safety)

## Primeiros Passos

### Pré-requisitos

- Android Studio Koala ou mais recente.
- Android SDK 37 (Compilação/Alvo).
- Um dispositivo Android físico (recomendado) ou Emulador com suporte a Camera2.

### Compilar e Executar

1. Clone o repositório:
   ```bash
   git clone https://github.com/zoozooll/AndroidCameraParameters.git
   ```
2. Abra o projeto no Android Studio.
3. Compile o projeto:
   ```bash
   ./gradlew assembleDebug
   ```
4. Instale e execute no seu dispositivo.

## Licença

Este projeto está licenciado sob a **Licença MIT**. Veja o arquivo [LICENSE](../../LICENSE) para mais detalhes.

## Suporte ou Contato

E-mail: kangkang365@gmail.com
Site do Projeto: [Documentação do Android Camera Parameters](https://zoozooll.github.io/AndroidCameraParameters/)
