package se.ugli.habanero.j.internal;

import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.SQLException;

import se.ugli.habanero.j.Habanero;
import se.ugli.habanero.j.OutParam;
import se.ugli.habanero.j.TypeAdaptor;

public class PrepareArgumentsCommand {

	public static PrepareArgumentsCommand apply(final PreparedStatement statement) {
		return new PrepareArgumentsCommand(statement);
	}

	private final PreparedStatement statement;

	private PrepareArgumentsCommand(final PreparedStatement statement) {
		this.statement = statement;
	}

	public void exec(final Object... args) throws SQLException {
		int parameterIndex = 1;
		for (final Object arg : args)
			if (arg instanceof OutParam && statement instanceof CallableStatement) {
				final OutParam outParam = (OutParam) arg;
				final CallableStatement callableStatement = (CallableStatement) statement;
				callableStatement.registerOutParameter(parameterIndex, outParam.sqlType);
			} else
				statement.setObject(parameterIndex++, convertArgument(arg));
	}

	private Object convertArgument(final Object object) {
		final Class<?> type = object.getClass();
		final TypeAdaptor typeAdaptor = Habanero.getTypeAdaptor(type);
		return typeAdaptor.toJdbcValue(object);
	}

}
