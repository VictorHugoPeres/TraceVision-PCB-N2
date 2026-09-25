# Servidor Backend REST - TraceVision

**Disciplina:** Desenvolvimento de Software para Dispositivos Móveis (N2)  
**Professor:** Me. Sergio Souza Novak  
**Instituição:** Universidade de Rio Verde (UniRV)  
**Equipe:** TraceVision Team  

---

## 1. Visão Geral
Servidor de API RESTful desenvolvido em Python para persistência e consulta dos relatórios de inferência de Visão Computacional On-Device executados pelo aplicativo Android.

Utiliza banco de dados relacional **SQLite** (`tracevision.db`), modelado exatamente de acordo com o modelo conceitual (ER) e o diagrama de classes UML estipulado na especificação do trabalho.

---

## 2. Rotas da API REST

| Método | Endpoint | Descrição |
|---|---|---|
| `GET` | `/api/status` | Retorna o status operacional do servidor |
| `POST` | `/api/inferencia` | Recebe e persiste o payload JSON completo da sessão de inferência |
| `GET` | `/api/inferencia/historico` | Retorna a lista cronológica de todas as sessões salvas |
| `GET` | `/api/inferencia/{id}/caixas` | Retorna as caixas delimitadoras e telemetria de uma sessão específica |

---

## 3. Estrutura do Payload JSON (POST /api/inferencia)

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

## 4. Como Executar o Servidor

### No Windows:
Basta dar um duplo clique no arquivo:
```cmd
iniciar_servidor.bat
```
Ou via linha de comando no terminal:
```bash
python server.py
```

O servidor iniciará na porta `8080`:
- **Acesso local:** `http://localhost:8080/`
- **Acesso via Emulador Android Studio:** `http://10.0.2.2:8080/`
- **Acesso via Celular Físico (mesma rede Wi-Fi):** `http://<SEU_IP_LOCAL>:8080/` (ex: `http://192.168.1.100:8080/`)
