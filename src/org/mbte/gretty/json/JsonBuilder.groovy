/*
 * Copyright 2009-2010 MBTE Sweden AB.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
@Typed package org.mbte.gretty.json

import org.codehaus.jackson.JsonGenerator
import org.codehaus.jackson.map.MappingJsonFactory

abstract class JsonClosure {

    protected JsonGenerator gen

    abstract void define ()

    void call () {
        if(!gen)
            gen = JsonBuilder2.current.get()

        if(!gen)
            throw new IllegalStateException("Can't use JsonClosure outside of JsonBuilder")

        define ()
    }

    void invokeUnresolvedMethod(String name, Object obj) {
        if(obj == null) {
            gen.writeNullField name
            return
        }

        switch(obj) {
            case Closure:
                gen.writeObjectFieldStart(name)
                obj.call()
                gen.writeEndObject()
            break

            case JsonClosure:
                gen.writeObjectFieldStart(name)
                obj.gen = gen
                obj.define()
                obj.gen = null
                gen.writeEndObject()
            break

            case String:
                gen.writeStringField(name, obj)
            break

            case Number:
                gen.writeNumberField(name, obj)
            break

            case Map:
                gen.writeObjectFieldStart(name)
                for(e in obj.entrySet()) {
                    invokeUnresolvedMethod(e.key.toString(), e.value)
                }
                gen.writeEndObject()
            break

            case Iterable:
                gen.writeArrayFieldStart(name)
                iterate(obj)
                gen.writeEndArray()
            break

            case Object []:
                invokeUnresolvedMethod(name, obj.iterator())
            break

            case Boolean:
                gen.writeBooleanField(name, obj)
            break

            default:
                gen.writeObjectField(name, obj)
            break
        }
    }

    void iterate(Iterable obj) {
        for (e in obj) {
            if(e == null) {
                gen.writeNull()
                continue
            }

            switch (e) {
                case Closure:
                    gen.writeStartObject()
                    e.call()
                    gen.writeEndObject()
                    break

                case JsonClosure:
                    e.gen = gen
                    gen.writeStartObject()
                    e.define()
                    gen.writeEndObject()
                    e.gen = null
                    break

                case Map:
                    gen.writeStartObject()
                    for (ee in e.entrySet()) {
                        invokeUnresolvedMethod(ee.key.toString(), ee.value)
                    }
                    gen.writeEndObject()
                    break

                case String:
                    gen.writeString(e)
                    break

                case Number:
                    gen.writeNumber(e)
                    break

                case Boolean:
                    gen.writeBoolean(e)
                break

                case Iterable:
                    gen.writeStartArray()
                    iterate(e)
                    gen.writeEndArray()
                    return

                default:
                    gen.writeObject(e)
            }
        }
    }
}

class JsonBuilder2 {
    protected static ThreadLocal current = []

    private final MappingJsonFactory factory = []
    private final JsonGenerator gen

    JsonBuilder2(Writer out) {
        gen = factory.createJsonGenerator(out)
        gen.useDefaultPrettyPrinter()
    }

    void call(JsonClosure obj) {
        try {
            current.set(gen)

            gen.writeStartObject()
            obj ()
            gen.writeEndObject()
            gen.close ()
        }
        finally {
            current.remove()
        }
    }

    void invokeUnresolvedMethod(String name, JsonClosure obj) {
        call {
            gen.writeObjectFieldStart name
            obj ()
            gen.writeEndObject()
        }
    }
}

JsonClosure externalData = {
    additionalData {
        married true
        conferences(['JavaOne', 'Gr8conf'])
        projectRoles([
                'Groovy' : 'Despot',
                'Grails' : 'Commiter',
                'Gaelyk' : 'Lead'
        ])
    }

    whateverElseData (["xxxxxx", [x: 12, y:14], ['a', 'b', 'c']])
}

JsonBuilder2 builder = [new PrintWriter(System.out)]
builder.person {
    firstName 'Guillaume'
    lastName  'Laforge'
    address (
        city: 'Paris',
        country: 'France',
        zip: 12345,
    )

    externalData ()
}

builder = [new PrintWriter(System.out)]
builder {
    person {
        firstName 'Guillaume'
        lastName  'Laforge'
        address (
            city: 'Paris',
            country: 'France',
            zip: 12345,
        )

        externalData ()
    }
}
