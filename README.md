# Reistream

**Reistream** é um organizador privado de anime local para Android.

Ele encontra vídeos que já estão no aparelho, agrupa episódios por série e usa o player integrado
para reproduzir os seus arquivos locais. A biblioteca não envia seus vídeos, miniaturas ou nomes de
arquivos para a internet.

## Estado da migração

O carregamento automático de extensões de streaming foi desativado. A base da nova biblioteca local
usa o `MediaStore` do Android e preserva o player atual. Consulte o plano técnico em
[docs/local-anime-organizer.md](docs/local-anime-organizer.md).

## Desenvolvimento

```bash
./gradlew :app:assembleStableDebug
```

Para acessar a biblioteca local em Android 13 ou posterior, conceda a permissão de vídeos quando o
aplicativo solicitar.
