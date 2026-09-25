#!/usr/bin/env python3
"""
TraceVision - Backend REST API (Servidor de Persistência N2)
Disciplina: Desenvolvimento de Software para Dispositivos Móveis - UniRV
Prof. Me. Sergio Souza Novak

Implementação de API RESTful em Python nativo com banco de dados SQLite.
Rotas especificadas no Enunciado (Figura 1 e Seção 4):
  - POST /api/inferencia
  - GET  /api/inferencia/historico
  - GET  /api/inferencia/{id}/caixas
"""

import json
import sqlite3
import re
from http.server import HTTPServer, BaseHTTPRequestHandler
from urllib.parse import urlparse
import os

DB_FILE = os.path.join(os.path.dirname(os.path.abspath(__file__)), "tracevision.db")

def init_db():
    """Inicializa as tabelas do Banco de Dados conforme Diagrama Conceitual (Figura 2)"""
    conn = sqlite3.connect(DB_FILE)
    cursor = conn.cursor()
    
    # Tabela 1: SESSÃO_INFERÊNCIA
    cursor.execute("""
    CREATE TABLE IF NOT EXISTS sessao_inferencia (
        id_sessao TEXT PRIMARY KEY,
        data_hora TEXT NOT NULL,
        nome_modelo TEXT NOT NULL,
        tempo_execucao_ms INTEGER NOT NULL,
        total_objetos INTEGER NOT NULL,
        confianca_media REAL NOT NULL,
        largura_px INTEGER NOT NULL,
        altura_px INTEGER NOT NULL
    );
    """)

    # Tabela 2: CAIXA_DELIMITADORA
    cursor.execute("""
    CREATE TABLE IF NOT EXISTS caixa_delimitadora (
        id INTEGER PRIMARY KEY AUTOINCREMENT,
        id_sessao TEXT NOT NULL,
        id_caixa INTEGER NOT NULL,
        rotulo_classe TEXT NOT NULL,
        confianca REAL NOT NULL,
        largura_px REAL NOT NULL,
        altura_px REAL NOT NULL,
        centroide_x REAL NOT NULL,
        centroide_y REAL NOT NULL,
        area_px2 REAL NOT NULL,
        FOREIGN KEY (id_sessao) REFERENCES sessao_inferencia(id_sessao) ON DELETE CASCADE
    );
    """)

    conn.commit()
    conn.close()
    print(f"[BD] Banco de dados SQLite pronto em: {DB_FILE}")

class TraceVisionRequestHandler(BaseHTTPRequestHandler):

    def _set_cors_headers(self):
        self.send_header("Access-Control-Allow-Origin", "*")
        self.send_header("Access-Control-Allow-Methods", "GET, POST, OPTIONS")
        self.send_header("Access-Control-Allow-Headers", "Content-Type, Authorization")

    def do_OPTIONS(self):
        self.send_response(200)
        self._set_cors_headers()
        self.end_headers()

    def do_GET(self):
        parsed = urlparse(self.path)
        path = parsed.path.rstrip("/")

        # Rota de Status / Home
        if path == "" or path == "/api" or path == "/api/status":
            self.send_response(200)
            self.send_header("Content-Type", "application/json; charset=utf-8")
            self._set_cors_headers()
            self.end_headers()
            response = {
                "app": "TraceVision Backend API",
                "status": "online",
                "versao": "1.0",
                "disciplina": "Desenvolvimento de Software para Dispositivos Móveis - N2"
            }
            self.wfile.write(json.dumps(response, indent=2).encode("utf-8"))
            return

        # RF7 / Figura 1: GET /api/inferencia/historico
        if path == "/api/inferencia/historico":
            self.handle_get_history()
            return

        # RF7 / Figura 1: GET /api/inferencia/{id}/caixas
        match = re.match(r"^/api/inferencia/([^/]+)/caixas$", path)
        if match:
            session_id = match.group(1)
            self.handle_get_boxes(session_id)
            return

        # Rota não encontrada
        self.send_response(404)
        self.send_header("Content-Type", "application/json")
        self._set_cors_headers()
        self.end_headers()
        self.wfile.write(json.dumps({"error": "Rota não encontrada"}).encode("utf-8"))

    def do_POST(self):
        parsed = urlparse(self.path)
        path = parsed.path.rstrip("/")

        # RF6 / Figura 1: POST /api/inferencia
        if path == "/api/inferencia":
            self.handle_post_inference()
            return

        self.send_response(404)
        self.send_header("Content-Type", "application/json")
        self._set_cors_headers()
        self.end_headers()
        self.wfile.write(json.dumps({"error": "Rota não encontrada"}).encode("utf-8"))

    def handle_post_inference(self):
        content_length = int(self.headers.get("Content-Length", 0))
        post_data = self.rfile.read(content_length)
        
        try:
            payload = json.loads(post_data.decode("utf-8"))
        except Exception as e:
            self.send_response(400)
            self.send_header("Content-Type", "application/json")
            self._set_cors_headers()
            self.end_headers()
            self.wfile.write(json.dumps({"error": f"JSON inválido: {str(e)}"}).encode("utf-8"))
            return

        id_sessao = payload.get("id_sessao") or payload.get("sessionId")
        data_hora = payload.get("data_hora") or payload.get("timestamp")
        nome_modelo = payload.get("nome_modelo") or payload.get("modelName", "best.onnx")
        tempo_ms = payload.get("tempo_execucao_ms") or payload.get("executionTimeMs", 0)
        total_objetos = payload.get("total_objetos") or payload.get("totalObjects", 0)
        confianca_media = payload.get("confianca_media") or payload.get("averageConfidence", 0.0)

        dimensao = payload.get("dimensao_imagem") or {}
        largura_px = dimensao.get("largura_px") or payload.get("imageWidth", 1080)
        altura_px = dimensao.get("altura_px") or payload.get("imageHeight", 1920)

        caixas = payload.get("caixas_delimitadoras") or payload.get("boundingBoxes", [])

        conn = sqlite3.connect(DB_FILE)
        cursor = conn.cursor()

        try:
            # Insere ou atualiza a sessão
            cursor.execute("""
            INSERT OR REPLACE INTO sessao_inferencia 
            (id_sessao, data_hora, nome_modelo, tempo_execucao_ms, total_objetos, confianca_media, largura_px, altura_px)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?)
            """, (id_sessao, data_hora, nome_modelo, tempo_ms, total_objetos, confianca_media, largura_px, altura_px))

            # Remove caixas anteriores caso esteja atualizando
            cursor.execute("DELETE FROM caixa_delimitadora WHERE id_sessao = ?", (id_sessao,))

            # Insere as caixas delimitadoras
            for caixa in caixas:
                id_caixa = caixa.get("id_caixa") or caixa.get("boxId", 1)
                rotulo = caixa.get("rotulo_classe") or caixa.get("classLabel", "defeito")
                conf = caixa.get("confianca") or caixa.get("confidence", 0.0)
                w = caixa.get("largura_px") or caixa.get("widthPx", 0.0)
                h = caixa.get("altura_px") or caixa.get("heightPx", 0.0)
                cx = caixa.get("centroide_x") or caixa.get("centroidX", 0.0)
                cy = caixa.get("centroide_y") or caixa.get("centroidY", 0.0)
                area = caixa.get("area_px2") or caixa.get("areaPx", w * h)

                cursor.execute("""
                INSERT INTO caixa_delimitadora
                (id_sessao, id_caixa, rotulo_classe, confianca, largura_px, altura_px, centroide_x, centroide_y, area_px2)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
                """, (id_sessao, id_caixa, rotulo, conf, w, h, cx, cy, area))

            conn.commit()
            print(f"[INFERENCIA SALVA] Sessao: {id_sessao} | Caixas: {len(caixas)} | Tempo: {tempo_ms}ms")

            self.send_response(201)
            self.send_header("Content-Type", "application/json")
            self._set_cors_headers()
            self.end_headers()
            self.wfile.write(json.dumps({
                "status": "success",
                "mensagem": "Inferência persistida com sucesso no Banco de Dados",
                "id_sessao": id_sessao
            }).encode("utf-8"))

        except Exception as e:
            conn.rollback()
            self.send_response(500)
            self.send_header("Content-Type", "application/json")
            self._set_cors_headers()
            self.end_headers()
            self.wfile.write(json.dumps({"error": f"Erro ao salvar no banco: {str(e)}"}).encode("utf-8"))
        finally:
            conn.close()

    def handle_get_history(self):
        conn = sqlite3.connect(DB_FILE)
        cursor = conn.cursor()

        cursor.execute("""
        SELECT id_sessao, data_hora, nome_modelo, tempo_execucao_ms, total_objetos, confianca_media, largura_px, altura_px
        FROM sessao_inferencia
        ORDER BY data_hora DESC
        """)
        rows = cursor.fetchall()

        history = []
        for row in rows:
            id_sessao = row[0]
            # Busca as caixas da sessão
            cursor.execute("""
            SELECT id_caixa, rotulo_classe, confianca, largura_px, altura_px, centroide_x, centroide_y, area_px2
            FROM caixa_delimitadora
            WHERE id_sessao = ?
            ORDER BY id_caixa ASC
            """, (id_sessao,))
            box_rows = cursor.fetchall()

            caixas = []
            for b in box_rows:
                caixas.append({
                    "id_caixa": b[0],
                    "rotulo_classe": b[1],
                    "confianca": round(b[2], 4),
                    "largura_px": round(b[3], 2),
                    "altura_px": round(b[4], 2),
                    "centroide_x": round(b[5], 2),
                    "centroide_y": round(b[6], 2),
                    "area_px2": round(b[7], 2)
                })

            history.append({
                "id_sessao": row[0],
                "data_hora": row[1],
                "nome_modelo": row[2],
                "tempo_execucao_ms": row[3],
                "total_objetos": row[4],
                "confianca_media": round(row[5], 4),
                "dimensao_imagem": {
                    "largura_px": row[6],
                    "altura_px": row[7]
                },
                "caixas_delimitadoras": caixas
            })

        conn.close()

        self.send_response(200)
        self.send_header("Content-Type", "application/json; charset=utf-8")
        self._set_cors_headers()
        self.end_headers()
        self.wfile.write(json.dumps(history, indent=2).encode("utf-8"))

    def handle_get_boxes(self, session_id):
        conn = sqlite3.connect(DB_FILE)
        cursor = conn.cursor()

        cursor.execute("""
        SELECT id_caixa, rotulo_classe, confianca, largura_px, altura_px, centroide_x, centroide_y, area_px2
        FROM caixa_delimitadora
        WHERE id_sessao = ?
        ORDER BY id_caixa ASC
        """, (session_id,))
        box_rows = cursor.fetchall()
        conn.close()

        caixas = []
        for b in box_rows:
            caixas.append({
                "id_caixa": b[0],
                "rotulo_classe": b[1],
                "confianca": round(b[2], 4),
                "largura_px": round(b[3], 2),
                "altura_px": round(b[4], 2),
                "centroide_x": round(b[5], 2),
                "centroide_y": round(b[6], 2),
                "area_px2": round(b[7], 2)
            })

        self.send_response(200)
        self.send_header("Content-Type", "application/json; charset=utf-8")
        self._set_cors_headers()
        self.end_headers()
        self.wfile.write(json.dumps(caixas, indent=2).encode("utf-8"))

def run_server(port=8080):
    init_db()
    server_address = ("", port)
    httpd = HTTPServer(server_address, TraceVisionRequestHandler)
    print(f"==================================================")
    print(f"[OK] Servidor TraceVision REST API iniciado na porta {port}")
    print(f"[OK] Enderecos de Acesso:")
    print(f"   - Local:    http://localhost:{port}/")
    print(f"   - Emulador: http://10.0.2.2:{port}/")
    print(f"   - Historico: http://localhost:{port}/api/inferencia/historico")
    print(f"==================================================")
    try:
        httpd.serve_forever()
    except KeyboardInterrupt:
        print("\n[INFO] Encerrando servidor.")
        httpd.server_close()

if __name__ == "__main__":
    run_server(8080)
