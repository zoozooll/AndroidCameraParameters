---
sidebar_position: 3
description: Práticas de segurança de dados e privacidade para o app Android Camera Parameters. Saiba mais sobre as permissões da câmera e como os metadados de hardware são manipulados.
keywords: [segurança de dados, política de privacidade, permissões do android]
---

# Guia de Segurança de Dados

Este documento descreve as práticas de coleta de dados e privacidade da aplicação Android Camera Parameters.

## Visão Geral

O Android Camera Parameters foi projetado com uma abordagem de "Privacidade em Primeiro Lugar". Como uma ferramenta de diagnóstico, ele precisa acessar informações de hardware para funcionar, mas não coleta nem transmite dados pessoais.

## Permissões

### Permissão de Câmera (`android.permission.CAMERA`)
- **Requisito**: Necessário para acessar o `CameraManager` e recuperar as `CameraCharacteristics`.
- **Uso**: O app apenas lê metadados de hardware. **Não grava** vídeo nem tira fotos sem uma ação explícita do usuário (por exemplo, em versões futuras se o teste de captura de imagem for adicionado).

## Coleta de Dados

- **Informações Pessoais**: O app **não coleta** nomes, endereços de e-mail, números de telefone ou quaisquer outros identificadores pessoais.
- **Dados de Localização**: O app **não acessa** seu GPS ou localização de rede.
- **Metadados de Hardware**: O app lê as especificações técnicas das lentes da sua câmera (resolução, distância focal, modos suportados). Esses dados permanecem no seu dispositivo, a menos que você use explicitamente o recurso "Exportar JSON" para compartilhá-los.

## Compartilhamento de Dados

A aplicação **não compartilha** nenhum dado com terceiros. Não há SDKs de rastreamento (como Firebase Analytics ou Facebook SDK) integrados no app principal.

## Controle do Usuário

- **Exportação JSON**: Os usuários podem optar por copiar ou compartilhar o JSON de parâmetros brutos da câmera. Isso é inteiramente iniciado pelo usuário.
- **Permissões**: Você pode revogar a permissão de Câmera a qualquer momento através das Configurações do Sistema Android, embora o app não consiga exibir detalhes da câmera sem ela.
