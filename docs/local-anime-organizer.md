# Organizador local de anime

O Rei-Stream está migrando de um catálogo baseado em extensões para uma biblioteca local.
O player existente continua sendo o player usado para abrir os episódios; o organizador só
descobre e agrupa os vídeos já presentes no aparelho.

## Privacidade e permissão

O primeiro passo é pedir a permissão de vídeos do Android (`READ_MEDIA_VIDEO`, ou a permissão
de armazenamento em aparelhos antigos). A varredura lê o índice `MediaStore`: nome, tamanho,
duração e data de modificação. Ela usa `content://` URIs, não caminhos absolutos, e não envia
arquivos, miniaturas ou nomes para a rede.

## Como os títulos são organizados

`LocalAnimeTitleParser` remove grupos de release, codecs e resoluções e separa sufixos comuns de
episódio. Em seguida, `LocalAnimeCatalog` agrupa episódios pela chave normalizada do título. O
parser é propositalmente conservador: ele não tenta adivinhar gênero, sinopse ou uma capa.

## Próximas etapas da migração

1. Exibir as séries retornadas por `LocalAnimeCatalog` na aba Biblioteca e abrir seus URIs no
   player atual.
2. Adicionar uma fonte de metadados **opt-in** com cache persistente: uma busca por série e uma
   capa baixada uma única vez. Sem uma confirmação do usuário, não haverá consulta à internet.
3. Adicionar o classificador inteligente local para sugerir agrupamentos e permitir corrigir
   título/gênero. Correções manuais terão prioridade e nenhuma sugestão será aplicada em silêncio.
4. Depois que a biblioteca local substituir as telas, retirar o carregamento, repositórios e UI
   de plugins em uma migração separada e testável.
