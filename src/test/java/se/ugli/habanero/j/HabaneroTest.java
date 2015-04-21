package se.ugli.habanero.j;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static se.ugli.habanero.j.internal.CloseUtil.close;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;

import org.junit.Before;
import org.junit.Test;

import se.ugli.commons.Option;

import com.google.common.collect.Iterables;

public class HabaneroTest {

	private final Habanero habanero = Habanero.apply();

	@Before
	public void setup() throws Exception {
		final Connection connection = habanero.dataSource.getConnection();
		final SqlScript script = SqlScript.apply(habanero.dataSource);
		script.run(getClass().getResourceAsStream("/person.sql"));
		DbUnitLoader.loadFromResource(habanero.dataSource, "/person.xml");
		close(connection);
	}

	@Test
	public void queryManyByType() {
		final String sql = "select name from person";
		final Iterable<String> names = habanero.queryMany(String.class, sql);
		assertEquals(2, Iterables.size(names));
		assertEquals("fredde", Iterables.get(names, 0));
		assertEquals("banan", Iterables.get(names, 1));
	}

	@Test(expected = HabaneroException.class)
	public void shouldQueryManyGenerateSqlError() {
		final String sql = "select name from persons";
		habanero.queryMany(String.class, sql);
	}

	private class Address {

		final String street;

		Address(final String street) {
			this.street = street;
		}

	}

	private class Person {
		final String name;
		final int age;
		final Iterable<Address> adresses;

		Person(final String name, final int age, final Iterable<Address> adresses) {
			this.name = name;
			this.age = age;
			this.adresses = adresses;
		}

	}

	@Test
	public void queryManyResultSetTransform() {
		final String sql = "select name,age from person";
		final Iterable<Person> persons = habanero.queryMany(new ResultSetIterator<Person>() {

			@Override
			public Person nextObject(final ResultSet input) throws SQLException {
				final String name = input.getString("name");
				final int age = input.getInt("age");
				return new Person(name, age, null);
			}
		}, sql);
		assertEquals(2, Iterables.size(persons));
		assertEquals("fredde", Iterables.get(persons, 0).name);
		assertEquals(44, Iterables.get(persons, 0).age);
		assertEquals("banan", Iterables.get(persons, 1).name);
		assertEquals(2, Iterables.get(persons, 1).age);
	}

	@Test
	public void queryManyResultMapTransform() {
		final String sql = "select name,age from person";
		final Iterable<Person> persons = habanero.queryMany(new TypedMapIterator<Person>() {

			@Override
			public Person nextObject(final TypedMap input) {
				final String name = input.get("name");
				final int age = input.get("age");
				return new Person(name, age, null);
			}

		}, sql);
		assertEquals(2, Iterables.size(persons));
		assertEquals("fredde", Iterables.get(persons, 0).name);
		assertEquals(44, Iterables.get(persons, 0).age);
		assertEquals("banan", Iterables.get(persons, 1).name);
		assertEquals(2, Iterables.get(persons, 1).age);
	}

	private class PersonGroupFunc extends GroupFunction<Person> {

		public PersonGroupFunc() {
			super("name", "age");
		}

		@Override
		protected Option<Person> createObject(final GroupContext cxt, final TypedMap typedMap) {
			final String name = typedMap.get("name");
			final int age = typedMap.get("age");
			final Iterable<Address> adresses = group(cxt, new GroupFunction<Address>("street") {

				@Override
				protected Option<Address> createObject(final GroupContext cxt, final TypedMap typedMap) {
					final String street = typedMap.get("street");
					if (street != null)
						return Option.apply(new Address(street));
					return Option.none();
				}
			});
			return Option.apply(new Person(name, age, adresses));
		}
	}

	@Test
	public void queryManyGroup() {
		String sql = "select name,age,street from person ";
		sql += "left join address on name=person";
		final Iterable<Person> persons = habanero.queryMany(new PersonGroupFunc(), sql);
		assertEquals(2, Iterables.size(persons));
		{
			final Person person = Iterables.get(persons, 0);
			assertEquals("fredde", person.name);
			assertEquals(44, person.age);
			assertEquals(1, Iterables.size(person.adresses));
			assertEquals("abc", person.adresses.iterator().next().street);
		}
		{
			final Person person = Iterables.get(persons, 1);
			assertEquals("banan", person.name);
			assertEquals(2, person.age);
			assertEquals(0, Iterables.size(person.adresses));
		}
	}

	@Test
	public void queryOneGroup() {
		String sql = "select name,age,street from person ";
		sql += "left join address on name=person ";
		sql += "where name=?";
		final Option<Person> opt = habanero.queryOne(new PersonGroupFunc(), sql, "fredde");
		assertTrue(opt.isDefined());
		final Person person = opt.get();
		assertEquals("fredde", person.name);
		assertEquals(44, person.age);
		assertEquals(1, Iterables.size(person.adresses));
		assertEquals("abc", person.adresses.iterator().next().street);
	}

	@Test
	public void queryOneGroupNotFound() {
		final String sql = "select name,age from person where name=?";
		assertFalse(habanero.queryOne(new PersonGroupFunc(), sql, "laban").isDefined());
	}

	@Test
	public void shouldHandleCRUD() {
		assertFalse(habanero.queryOne(Integer.class, "select age from person where name=?", "ubbe").isDefined());
		assertEquals(1, habanero.update("insert into person(name,age) values(?,?)", "ubbe", 10));
		assertEquals(new Integer(10), habanero.queryOne(Integer.class, "select age from person where name=?", "ubbe")
				.get());
		assertEquals(1, habanero.update("update person set age=? where name=?", 43, "ubbe"));
		assertEquals(new Integer(43), habanero.queryOne(Integer.class, "select age from person where name=?", "ubbe")
				.get());
		assertEquals(1, habanero.update("delete from person where name=?", "ubbe"));
		assertFalse(habanero.queryOne(Integer.class, "select age from person where name=?", "ubbe").isDefined());
	}

	@Test(expected = HabaneroException.class)
	public void shoulUpdateSqlError() {
		habanero.update("delete from persons where name=?", "ubbe");
	}
}
