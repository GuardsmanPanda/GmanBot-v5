import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import javax.sql.rowset.CachedRowSet;
import javax.sql.rowset.RowSetProvider;
import java.sql.*;
import java.util.Arrays;


public class Database {
    private static final HikariDataSource dbSource;

    static {
        var config = new HikariConfig();
        config.setDataSourceClassName("org.postgresql.ds.PGSimpleDataSource");
        config.setUsername("gbot");
        config.setPassword(Config.file_env("db_pass"));
        dbSource = new HikariDataSource(config);
        /*try {
            Class.forName("org.postgresql.Driver");
            c = DriverManager.getConnection("jdbc:postgresql://localhost:5432/gbot", "gbot", "secretpass123");
        } catch (Exception e) {
            e.printStackTrace();
            System.err.println(e.getClass().getName()+": "+e.getMessage());
            System.exit(0);
        }*/
        System.out.println("Opened database successfully");
    }

    public static int executePreparedSQL(String sql, Object... arguments) {
        try (var con = dbSource.getConnection()) {
            var  statement = con.prepareStatement(sql);
            for (int i = 0; i < arguments.length; i++) statement.setObject(i + 1, arguments[i]);
            return statement.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
            return -1;
        }
    }

    public static CachedRowSet getRowSetFromSQL(String sql, Object... arguments) {
        return getSqlThingy(sql, arguments, -1, r -> {
            CachedRowSet res = RowSetProvider.newFactory().createCachedRowSet();
            res.populate(r);
            return res;
        });
    }

    @SuppressWarnings("unchecked")
    public static <E, F, G> Triple<E, F, G> getTripleFromSql(String sql, Object... arguments) {
        return getSqlThingy(sql, arguments, 3,
                r -> new Triple<>((E)r.getObject(1), (F)r.getObject(2), (G)r.getObject(3)));
    }

    @SuppressWarnings("unchecked")
    public static <E, F> Pair<E, F> getPairFromSql(String sql, Object... arguments) {
        return getSqlThingy(sql, arguments, 2,
                r -> new Pair<>((E)r.getObject(1), (F)r.getObject(2)));
    }

    public static <E> E getValueFromSQL(String sql, Class<E> returnType, Object... arguments) {
        return getSqlThingy(sql, arguments, 1, r -> returnType.cast(r.getObject(1)));
    }


    private static <E> E getSqlThingy(String sql, Object[] arguments, int expectedColumns, SQLFunction<ResultSet, E> gen) {
        try (var con = dbSource.getConnection()) {
            var statement = con.prepareStatement(sql);
            for (int i = 0; i < arguments.length; i++) statement.setObject(i + 1, arguments[i]);
            ResultSet tmp = statement.executeQuery();
            assert (expectedColumns == -1 || tmp.getMetaData().getColumnCount() == expectedColumns);
            if (expectedColumns != -1 && !tmp.next()) return null;
            E res = gen.apply(tmp);
            if (tmp.next()) throw new RuntimeException("Unconsumed rows left in result object: " + sql + Arrays.toString(arguments));
            statement.close();
            return res;
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("Some SQL went horribly wrong: " + sql + Arrays.toString(arguments));
        }
    }

    @FunctionalInterface
    private interface SQLFunction<T, R> {
        R apply(T t) throws SQLException;
    }
}
