package com.smartgov.lez.core.output.establishment;

import java.io.IOException;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;
import com.smartgov.lez.core.agent.establishment.Establishment;

public class EstablishmentIdSerializer extends StdSerializer<Establishment> {

	private static final long serialVersionUID = 1L;
	
	public EstablishmentIdSerializer() {
		this(null);
	}
	protected EstablishmentIdSerializer(Class<Establishment> t) {
		super(t);
	}
	@Override
	public void serialize(Establishment value, JsonGenerator gen, SerializerProvider provider) throws IOException {
		gen.writeObject(value.getId());
	}

}
