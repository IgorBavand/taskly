package app.repository

import app.model.Todo
import java.time.LocalDateTime
import javax.sql.DataSource

class TodoRepository(private val dataSource: DataSource) {

    fun findAll(): List<Todo> {
        val connection = dataSource.connection
        val stmt = connection.prepareStatement("SELECT id, title, done FROM todos")

        val rs = stmt.executeQuery()
        val todos = mutableListOf<Todo>()

        while (rs.next()) {
            todos.add(
                Todo(
                    rs.getLong("id"),
                    rs.getString("title"),
                    rs.getBoolean("done"),
                    LocalDateTime.now()
                )
            )
        }

        rs.close()
        stmt.close()
        connection.close()

        return todos
    }

    fun findById(id: Long): Todo? {
        val conn = dataSource.connection
        val stmt = conn.prepareStatement(
            "SELECT id, title, done FROM todos WHERE id = ?"
        )

        stmt.setLong(1, id)

        val rs = stmt.executeQuery()

        val todo = if (rs.next()) {
            Todo(
                rs.getLong("id"),
                rs.getString("title"),
                rs.getBoolean("done"),
                LocalDateTime.now()
            )
        } else null

        rs.close()
        stmt.close()
        conn.close()

        return todo
    }

    fun create(title: String): Todo {
        val conn = dataSource.connection

        val stmt = conn.prepareStatement(
            "INSERT INTO todos (title) VALUES (?) RETURNING id, title, done"
        )

        stmt.setString(1, title)

        val rs = stmt.executeQuery()
        rs.next()

        val todo = Todo(
            rs.getLong("id"),
            rs.getString("title"),
            rs.getBoolean("done"),
            LocalDateTime.now()
        )

        rs.close()
        stmt.close()
        conn.close()

        return todo
    }

    fun toggle(id: Long): Todo {
        val conn = dataSource.connection

        val stmt = conn.prepareStatement(
            """
            UPDATE todos
            SET done = NOT done
            WHERE id = ?
            RETURNING id, title, done
            """.trimIndent()
        )

        stmt.setLong(1, id)

        val rs = stmt.executeQuery()
        rs.next()

        val todo = Todo(
            rs.getLong("id"),
            rs.getString("title"),
            rs.getBoolean("done"),
            LocalDateTime.now()
        )

        rs.close()
        stmt.close()
        conn.close()

        return todo
    }

    fun delete(id: Long) {
        val conn = dataSource.connection
        val stmt = conn.prepareStatement("DELETE FROM todos WHERE id = ?")

        stmt.setLong(1, id)
        stmt.executeUpdate()

        stmt.close()
        conn.close()
    }
}