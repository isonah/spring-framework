package org.springframework.ui.format;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import java.math.BigDecimal;
import java.text.ParseException;
import java.util.Locale;

import org.junit.Before;
import org.junit.Test;
import org.springframework.core.convert.TypeDescriptor;
import org.springframework.core.style.ToStringCreator;
import org.springframework.ui.format.number.CurrencyFormat;
import org.springframework.ui.format.number.CurrencyFormatter;
import org.springframework.ui.format.number.IntegerFormatter;

public class GenericFormatterRegistryTests {

	private GenericFormatterRegistry registry;

	@Before
	public void setUp() {
		registry = new GenericFormatterRegistry();
	}

	@Test
	public void testAdd() {
		registry.add(new IntegerFormatter());
		Formatter formatter = registry.getFormatter(typeDescriptor(Long.class));
		String formatted = formatter.format(new Long(3), Locale.US);
		assertEquals("3", formatted);
	}

	@Test
	public void testAddByObjectType() {
		registry.add(Integer.class, new IntegerFormatter());
		Formatter formatter = registry.getFormatter(typeDescriptor(Integer.class));
		String formatted = formatter.format(new Integer(3), Locale.US);
		assertEquals("3", formatted);
	}

	@Test
	public void testAddAnnotationFormatterFactory() throws Exception {
		registry.add(new CurrencyAnnotationFormatterFactory());
		Formatter formatter = registry.getFormatter(new TypeDescriptor(getClass().getField("currencyField")));
		String formatted = formatter.format(new BigDecimal("5.00"), Locale.US);
		assertEquals("$5.00", formatted);
	}

	@Test
	public void testGetDefaultFormatterForType() {
		Formatter formatter = registry.getFormatter(typeDescriptor(Address.class));
		Address address = new Address();
		address.street = "12345 Bel Aire Estates";
		address.city = "Palm Bay";
		address.state = "FL";
		address.zip = "12345";
		String formatted = formatter.format(address, Locale.US);
		assertEquals("12345 Bel Aire Estates:Palm Bay:FL:12345", formatted);
	}
	
	@Test
	public void testGetNoFormatterForType() {
		assertNull(registry.getFormatter(typeDescriptor(Integer.class)));
	}
	
	@CurrencyFormat
	public BigDecimal currencyField;

	private static TypeDescriptor typeDescriptor(Class<?> clazz) {
		return TypeDescriptor.valueOf(clazz);
	}

	public static class CurrencyAnnotationFormatterFactory implements
			AnnotationFormatterFactory<CurrencyFormat, BigDecimal> {
		public Formatter<BigDecimal> getFormatter(CurrencyFormat annotation) {
			return new CurrencyFormatter();
		}
	}
	
	@Formatted(AddressFormatter.class)
	public static class Address {
		private String street;
		private String city;
		private String state;
		private String zip;
		private String country;

		public String getStreet() {
			return street;
		}

		public void setStreet(String street) {
			this.street = street;
		}

		public String getCity() {
			return city;
		}

		public void setCity(String city) {
			this.city = city;
		}

		public String getState() {
			return state;
		}

		public void setState(String state) {
			this.state = state;
		}

		public String getZip() {
			return zip;
		}

		public void setZip(String zip) {
			this.zip = zip;
		}

		public String getCountry() {
			return country;
		}

		public void setCountry(String country) {
			this.country = country;
		}

		public String toString() {
			return new ToStringCreator(this).append("street", street).append("city", city).append("state", state)
					.append("zip", zip).toString();
		}
	}
	
	public static class AddressFormatter implements Formatter<Address> {

		public String format(Address address, Locale locale) {
			return address.getStreet() + ":" + address.getCity() + ":" + address.getState() + ":" + address.getZip();
		}

		public Address parse(String formatted, Locale locale) throws ParseException {
			Address address = new Address();
			String[] fields = formatted.split(":");
			address.setStreet(fields[0]);
			address.setCity(fields[1]);
			address.setState(fields[2]);
			address.setZip(fields[3]);
			return address;
		}

	}

}