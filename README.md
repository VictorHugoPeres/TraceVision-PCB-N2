# TraceVision: Inspeção de Placas PCB com Visão Computacional On-Device & YOLO ONNX

**Disciplina:** DESENVOLVIMENTO DE SOFTWARE PARA DISPOSITIVOS MÓVEIS (Trabalho Avaliativo N2)  
**Professor:** Me. Sergio Souza Novak  
**Instituição:** Universidade de Rio Verde (UniRV)  

---

## 1. Visão Geral do Projeto
O **TraceVision** é uma solução computacional completa (*Full-Stack Mobile*) desenvolvida em **Kotlin** com **Jetpack Compose**, projetada para realizar o controle de qualidade automatizado e inspeção visual de placas de circuito impresso (PCBs).

Utilizando redes neurais YOLO convertidas para formato **ONNX** (`.onnx`), o aplicativo executa inferência de Visão Computacional diretamente no hardware do dispositivo (**On-Device**), garantindo operação offline rápida e segura para chão de fábrica, com sincronização automática com servidor backend REST e persistência em banco de dados SQLite.

---

## 2. Pilha Tecnológica & Arquitetura

- **UI Declarativa:** 100% Jetpack Compose (zero XML Views), implementando Design System *Industrial Tech / Clean Engineering* em estrito Light Mode (fundo Slate 50 `#F8FAFC`, cards em branco puro, bordas técnicas `#E2E8F0` e paleta de precisão em azul ciano `#0284C7`, verde esmeralda `#10B981` e alertas tipados).
- **Arquitetura Android:** MVVM (*Model-View-ViewModel*) com gerenciamento de estado reativo via `StateFlow`, fluxos assíncronos com Kotlin `Coroutines` e separação rigorosa em camadas.
- **Motor de IA Local:** `ONNX Runtime Android` (`com.microsoft.onnxruntime:onnxruntime-android:1.18.0`), responsável por carregar os pesos da rede (`best.onnx`) e executar inferência com entrada NCHW `[1, 3, 640, 640]`.
- **Servidor Backend REST:** API RESTful desenvolvida em Python com persistência em banco de dados relacional **SQLite** (`backend/server.py`).
- **Comunicação REST:** Retrofit2 + OkHttp + Gson com suporte a troca dinâmica de URL base.
- **Carregamento de Imagens:** Coil Compose e APIs nativas do Android (Bitmap, Canvas, FileProvider).

---

## 3. As 4 Telas Principais (Bottom Navigation)

| Tela | Nome | Funcionalidades |
|---|---|---|
| **Tela 1** | **Configuração & Entrada** (`ConfigScreen`) | Seleção do modelo YOLO ONNX (embutido ou SAF), captura via Câmera Nativa ou Galeria, Slider de Limiar de Confiança e Pré-visualização da amostra. |
| **Tela 2** | **Resultado Gráfico** (`ResultScreen`) | Cards de destaque com Total de Defeitos e Confiança Média, tempo de execução em ms, imagem anotada com Canvas Compose dinâmico (caixas, classes e centróides) e despacho automático para o servidor backend. |
| **Tela 3** | **Histórico de Auditoria** (`HistoryScreen`) | Mini dashboard consolidado (Total de Inferências, Objetos Contados, Média de Tempo), campo de pesquisa/filtro, listagem cronológica com status de sincronização e navegação para detalhamento. |
| **Tela 4** | **Detalhamento Geométrico** (`DetailScreen`) | Auditoria individual de cada Bounding Box: ID único, rótulo da classe, confiança %, dimensões espaciais ($W_{px}$, $H_{px}$), centróide exato ($X_c = X_{min} + \frac{W_{px}}{2}$, $Y_c = Y_{min} + \frac{H_{px}}{2}$) e área total ($A_{px} = W_{px} \times H_{px}$). |

---

## 4. Estrutura do Payload JSON (Comunicação com o Backend)

```json
{
  "id_sessao": "sessao_20260918_001",
  "data_hora": "2026-09-18T23:06:00Z",
  "nome_modelo": "best.onnx",
  "tempo_execucao_ms": 909,
  "total_objetos": 3,
  "confianca_media": 0.91,
  "dimensao_imagem": {
    "largura_px": 1080,
    "altura_px": 1920
  },
  "caixas_delimitadoras": [
    {
      "id_caixa": 1,
      "rotulo_classe": "broken_circuit",
      "confianca": 0.93,
      "largura_px": 120.5,
      "altura_px": 45.0,
      "centroide_x": 210.25,
      "centroide_y": 422.50,
      "area_px2": 5422.5
    }
  ]
}
```

---

## 5. Como Executar

### 5.1. Servidor Backend
1. Entre na pasta `backend/` e execute o script:
   ```cmd
   iniciar_servidor.bat
   ```
   Ou via terminal:
   ```bash
   python backend/server.py
   ```
2. O servidor ficará ativo na porta `8080` com endpoints:
   - `POST /api/inferencia`
   - `GET  /api/inferencia/historico`
   - `GET  /api/inferencia/{id}/caixas`

### 5.2. Aplicativo Android
- Abra o projeto no **Android Studio**.
- Conecte um dispositivo físico ou inicie o Emulador.
- Execute a tarefa `Run 'app'`.
- O APK compilado de debug também está gerado em:
  `app/build/outputs/apk/debug/app-debug.apk`

---

## 6. Como Subir para o GitHub

Foi incluído um script automático na raiz do projeto:
- Dê um duplo-clique em: `subir_para_github.bat`
- Cole a URL do seu repositório novo criado no GitHub (`https://github.com/seu-usuario/nome-do-repo.git`) e pressione ENTER.
