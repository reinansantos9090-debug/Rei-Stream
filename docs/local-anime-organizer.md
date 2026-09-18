# Rei Stream — biblioteca pessoal local

Rei Stream reproduz **somente arquivos locais**. A internet é uma futura camada opt-in de metadados e capas; nenhum nome de arquivo é enviado automaticamente e ela nunca fornece o vídeo.

## Pastas e privacidade

`LocalLibraryRepository` usa o seletor de pastas do Android (Storage Access Framework). Cada URI de árvore é persistida com permissão somente de leitura e a varredura percorre apenas árvores escolhidas pelo usuário. Isso permite múltiplas pastas sem `MANAGE_EXTERNAL_STORAGE` para a biblioteca nova.

## Indexação

`LocalAnimeTitleParser` limpa grupos, resolução, codec e tags de release, reconhece `S01E03`, `EP03`, números simples, filmes, OVAs, ONAs e especiais. `LocalLibraryIndexer` agrupa pelo título normalizado, ordena temporada/episódio e sinaliza duplicatas sem apagar arquivos. A API retorna arquivos inacessíveis para uma tela de recuperação/manual association.

## Próximos passos de UI

A tela de Biblioteca deve abrir `ACTION_OPEN_DOCUMENT_TREE`, chamar `addFolder`, executar `scan` fora da UI e apresentar `LocalScanResult`. Os `content://` URI retornados são compatíveis com o `DownloadedPlayerActivity` existente, preservando controles, legendas e faixas locais. Metadados, capas e IA continuam deliberadamente opt-in, com cache persistente e sem chaves embutidas.
