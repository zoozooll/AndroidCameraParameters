---
sidebar_position: 3
description: Práticas de segurança de dados e privacidade para o aplicativo Android Camera Parameters. Saiba mais sobre as permissões de câmera e como os metadados de hardware são tratados.
keywords: [segurança de dados, política de privacidade, permissões android]
---

# Guia de Segurança de Dados

Este documento descreve as práticas de coleta de dados e privacidade do aplicativo Android Camera Parameters.

## Visão Geral

O Android Camera Parameters foi projetado com uma abordagem de "Privacidade em Primeiro Lugar". Como uma ferramenta de diagnóstico, ele precisa acessar informações de hardware para funcionar, mas não coleta nem transmite dados pessoais.

## Permissões

### Permissão de Câmera (`android.permission.CAMERA`)
- **Requisito**: Necessária para acessar o `CameraManager` e recuperar `CameraCharacteristics`.
- **Uso**: O aplicativo lê apenas metadados de hardware. Ele **não** grava vídeos nem tira fotos sem a ação explícita do usuário (ex: em versões futuras se testes de captura de imagem forem adicionados).

## Coleta de Dados

- **Informações Pessoais**: O aplicativo **não coleta** nomes, endereços de e-mail, números de telefone ou qualquer outro identificador pessoal.
- **Dados de Localização**: O aplicativo **não acessa** seu GPS ou localização de rede.
- **Metadados de Hardware**: O aplicativo lê as especificações técnicas das lentes da sua câmera (resolução, distância focal, modos suportados). Esses dados permanecem no seu dispositivo, a menos que você use explicitamente o recurso "Exportar JSON" para compartilhá-los.

## Compartilhamento de Dados

O aplicativo **não compartilha** nenhum dado com terceiros. Não há SDKs de rastreamento (como Firebase Analytics ou Facebook SDK) integrados ao núcleo do aplicativo.

## Controle do Usuário

- **Exportação JSON**: Os usuários podem optar por copiar ou compartilhar o JSON bruto dos parâmetros da câmera. Isso é inteiramente iniciado pelo usuário.
- **Permissões**: Você pode revogar a permissão de Câmera a qualquer momento através das Configurações do Sistema Android, embora o aplicativo não consiga exibir os detalhes da câmera sem ela.
