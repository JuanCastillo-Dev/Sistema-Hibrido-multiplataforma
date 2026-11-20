import os
from flask import Flask, render_template, request, redirect, url_for, flash
from dotenv import load_dotenv
import pymysql

load_dotenv()

app = Flask(__name__)
app.secret_key = os.getenv("SECRET_KEY", "clinica-veterinaria-secret-key")


def get_db_connection():
    return pymysql.connect(
        host=os.getenv("DB_HOST"),
        port=int(os.getenv("DB_PORT", "3306")),
        user=os.getenv("DB_USER"),
        password=os.getenv("DB_PASSWORD"),
        database=os.getenv("DB_NAME"),
        cursorclass=pymysql.cursors.DictCursor,
        autocommit=True,
    )


@app.route("/")
def index():
    conn = get_db_connection()
    try:
        with conn.cursor() as cursor:
            cursor.execute(
                """
                SELECT c.id_cita, c.id_mascota, c.motivo, c.fecha_cita, c.estado,
                       m.nombre AS mascota_nombre, m.especie, m.raza, m.propietario
                FROM citas c
                INNER JOIN mascotas m ON c.id_mascota = m.id_mascota
                ORDER BY c.fecha_cita DESC, c.id_cita DESC
                """
            )
            citas = cursor.fetchall()

            cursor.execute(
                """
                SELECT id_mascota, nombre, especie, propietario
                FROM mascotas
                ORDER BY nombre ASC
                """
            )
            mascotas = cursor.fetchall()
    finally:
        conn.close()

    return render_template("index.html", citas=citas, mascotas=mascotas, cita_editar=None)


@app.route("/citas/crear", methods=["POST"])
def crear_cita():
    id_mascota = request.form.get("id_mascota")
    motivo = request.form.get("motivo", "").strip()
    fecha_cita = request.form.get("fecha_cita", "").replace("T", " ").strip()
    estado = request.form.get("estado", "Programada")

    if len(fecha_cita) == 16:
        fecha_cita = f"{fecha_cita}:00"

    if not id_mascota or not motivo or not fecha_cita:
        flash("Completa mascota, motivo y fecha/hora de la cita.", "error")
        return redirect(url_for("index"))

    conn = get_db_connection()
    try:
        with conn.cursor() as cursor:
            cursor.execute(
                """
                INSERT INTO citas (id_mascota, motivo, fecha_cita, estado)
                VALUES (%s, %s, %s, %s)
                """,
                (int(id_mascota), motivo, fecha_cita, estado),
            )
        flash("Cita registrada correctamente.", "success")
    except Exception as exc:
        flash(f"Error al registrar la cita: {exc}", "error")
    finally:
        conn.close()

    return redirect(url_for("index"))


@app.route("/citas/editar/<int:id_cita>")
def editar_cita_form(id_cita):
    conn = get_db_connection()
    try:
        with conn.cursor() as cursor:
            cursor.execute(
                """
                SELECT c.id_cita, c.id_mascota, c.motivo, c.fecha_cita, c.estado,
                       m.nombre AS mascota_nombre, m.especie, m.raza, m.propietario
                FROM citas c
                INNER JOIN mascotas m ON c.id_mascota = m.id_mascota
                ORDER BY c.fecha_cita DESC, c.id_cita DESC
                """
            )
            citas = cursor.fetchall()

            cursor.execute(
                """
                SELECT id_mascota, nombre, especie, propietario
                FROM mascotas
                ORDER BY nombre ASC
                """
            )
            mascotas = cursor.fetchall()

            cursor.execute("SELECT * FROM citas WHERE id_cita = %s", (id_cita,))
            cita_editar = cursor.fetchone()
    finally:
        conn.close()

    if not cita_editar:
        flash("La cita seleccionada no existe.", "error")
        return redirect(url_for("index"))

    return render_template("index.html", citas=citas, mascotas=mascotas, cita_editar=cita_editar)


@app.route("/citas/actualizar/<int:id_cita>", methods=["POST"])
def actualizar_cita(id_cita):
    id_mascota = request.form.get("id_mascota")
    motivo = request.form.get("motivo", "").strip()
    fecha_cita = request.form.get("fecha_cita", "").replace("T", " ").strip()
    estado = request.form.get("estado", "Programada")

    if len(fecha_cita) == 16:
        fecha_cita = f"{fecha_cita}:00"

    conn = get_db_connection()
    try:
        with conn.cursor() as cursor:
            cursor.execute(
                """
                UPDATE citas
                SET id_mascota = %s, motivo = %s, fecha_cita = %s, estado = %s
                WHERE id_cita = %s
                """,
                (int(id_mascota), motivo, fecha_cita, estado, id_cita),
            )
        flash("Cita actualizada correctamente.", "success")
    except Exception as exc:
        flash(f"Error al actualizar la cita: {exc}", "error")
    finally:
        conn.close()

    return redirect(url_for("index"))


@app.route("/citas/eliminar/<int:id_cita>", methods=["POST"])
def eliminar_cita(id_cita):
    conn = get_db_connection()
    try:
        with conn.cursor() as cursor:
            cursor.execute("DELETE FROM citas WHERE id_cita = %s", (id_cita,))
        flash("Cita eliminada correctamente.", "success")
    except Exception as exc:
        flash(f"Error al eliminar la cita: {exc}", "error")
    finally:
        conn.close()

    return redirect(url_for("index"))


if __name__ == "__main__":
    port = int(os.getenv("FLASK_PORT", "5000"))
    app.run(host="0.0.0.0", port=port, debug=False)