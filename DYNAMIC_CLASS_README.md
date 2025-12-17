# Dynamisk Klassgenering med Groovy & XML-Marshall

En Spring Boot-applikation som demonstrerar **metaprogrammering i Groovy** för dynamisk klassgenering vid runtime.

## Arkitektur

```
com.example.dynamic/
├── controller/          REST-endpoints
├── groovy/             Groovy-kompilering & klassladning
├── model/              Data Transfer Objects
├── security/           Validering & säkerhet
└── xml/                XML-marshalling
```

## Funktionalitet

1. **Klassgenering från JSON**: Skicka en klasseskriptor via REST för att dynamiskt generera en Groovy-klass
2. **Runtime-kompilering**: Använder `GroovyClassLoader` för att kompilera och ladda klasser under körning
3. **Instansiering**: Skapar objektinstanser från genererade klasser
4. **XML-marshalling**: Konverterar objekt till XML-format med Jackson XML
5. **Caching**: SoftReference-baserad cache för att möjliggöra GC
6. **Säkerhet**: Validering mot injection via whitelist och regex

## REST API

### 1. Generera klass

```bash
POST /api/dynamic/class
Content-Type: application/json

{
  "className": "Person",
  "packageName": "com.example.dynamic.generated",
  "fields": [
    {"name": "firstName", "type": "String", "required": true},
    {"name": "lastName", "type": "String", "required": true},
    {"name": "age", "type": "int", "required": false},
    {"name": "address", "type": "String", "required": false}
  ]
}
```

**Response:**
```json
{
  "classId": "com.example.dynamic.generated.Person_1702886385123",
  "className": "Person",
  "fullyQualifiedName": "com.example.dynamic.generated.Person",
  "packageName": "com.example.dynamic.generated",
  "sourceCode": "package com.example.dynamic.generated\n...",
  "cacheStats": {
    "classCount": 1,
    "sourceCodeCount": 1
  }
}
```

### 2. Instansiera klass och marshalla till XML

```bash
POST /api/dynamic/instance
Content-Type: application/json

{
  "fullyQualifiedName": "com.example.dynamic.generated.Person",
  "sourceCode": "package com.example.dynamic.generated\n...",
  "data": {
    "firstName": "John",
    "lastName": "Doe",
    "age": 30,
    "address": "123 Main Street"
  }
}
```

**Response:**
```json
{
  "success": true,
  "fullyQualifiedName": "com.example.dynamic.generated.Person",
  "xml": "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n<Person>\n  <firstName>John</firstName>\n  ...",
  "cacheStats": {
    "classCount": 1,
    "sourceCodeCount": 1
  }
}
```

### 3. Hämta cache-statistik

```bash
GET /api/dynamic/cache-stats
```

**Response:**
```json
{
  "success": true,
  "cacheStats": {
    "classCount": 3,
    "sourceCodeCount": 3
  }
}
```

### 4. Rensa cache

```bash
DELETE /api/dynamic/cache
```

**Response:**
```json
{
  "success": true,
  "message": "Cache rensat framgångsrikt"
}
```

## Tillåtna Fälttyper

Validerat via whitelist i `ClassDescriptorValidator`:

- **Enkla typer**: `String`, `int`, `long`, `double`, `float`, `boolean`
- **Wrapper-klasser**: `Integer`, `Long`, `Double`, `Float`, `Boolean`
- **Datum/Tid**: `java.time.LocalDate`, `java.time.LocalDateTime`
- **Tal**: `java.math.BigDecimal`, `java.util.Date`

## Säkerhet

### Validering

1. **Klassnamn**: Måste börja med bokstav, innehålla endast bokstäver/siffror/underscore
2. **Paketnamn**: Måste följa Java-namnkonvention
3. **Fältnamn**: Samma regler som klassnamn
4. **Fälttyper**: Endast från whitelist (ingen Object, custom-klasser, etc.)
5. **Längdbegränsningar**: Max 255 tecken för namn, max 100 fält per klass
6. **Java-keywords**: Blockeras automatiskt (class, public, private, etc.)

### Classloader-isolering

- Varje instans av `DynamicClassLoader` har sin egen isolerad `GroovyClassLoader`
- `SoftReference` för cache möjliggör GC att frigöra minne när det behövs
- Max cache-storlek: 1000 klasser

## Exempel: Komplett Workflow

### Steg 1: Starta applikationen

```bash
mvn spring-boot:run
```

Servern startar på `http://localhost:8080`

### Steg 2: Skapa Person-klass

```bash
curl -X POST http://localhost:8080/api/dynamic/class \
  -H "Content-Type: application/json" \
  -d '{
    "className": "Person",
    "packageName": "com.example.dynamic.generated",
    "fields": [
      {"name": "firstName", "type": "String", "required": true},
      {"name": "lastName", "type": "String", "required": true},
      {"name": "age", "type": "int", "required": false},
      {"name": "address", "type": "String", "required": false}
    ]
  }'
```

**Utsignal**: Kopieras `sourceCode` från response

### Steg 3: Skapa instans och marshalla till XML

```bash
curl -X POST http://localhost:8080/api/dynamic/instance \
  -H "Content-Type: application/json" \
  -d '{
    "fullyQualifiedName": "com.example.dynamic.generated.Person",
    "sourceCode": "<kopiera från steg 2>",
    "data": {
      "firstName": "John",
      "lastName": "Doe",
      "age": 30,
      "address": "123 Main Street"
    }
  }'
```

**Resultat**: XML-representation av objektet

## Testning

Kör integrationtester:

```bash
mvn test -Dtest=DynamicClassGenerationTest
```

Tester validerar:
- ✅ Klassgenering från JSON
- ✅ XML-marshalling
- ✅ Cache-funktionalitet
- ✅ Säkerhetsvalidering (ogiltiga klassnamn)
- ✅ Typvalidering (otillåtna typer blockeras)
- ✅ Multi-class-generation med caching

## Arkitektur-komponenter

### `ClassDescriptor` & `FieldDefinition`
DTOs för klasseskriptorn från JSON

### `ClassDescriptorValidator`
Validering & säkerhet - whitelist, regex, length-checking

### `GroovyCodeGenerator`
Genererar Groovy-klasskällkod från descriptor
- Inkluderar getters, setters, toString, equals, hashCode
- JAXB & Jackson-annotationer för XML/JSON

### `DynamicClassLoader`
Hanterar Groovy-kompilering och klassladning
- Isolerad `GroovyClassLoader` per instans
- SoftReference-baserad cache (GC-friendly)
- SHA-256 cache-nycklar

### `DynamicClassService`
Orkestreringslagret som:
1. Validerar klasseskriptor
2. Genererar Groovy-kod
3. Kompilerar klassen
4. Instansierar
5. Mappar JSON-data via reflection
6. Marshaller till XML

### `DynamicClassController`
REST-endpoints för klientinteraktion

## Performance & Minneshantering

### Caching
- **SoftReference**: Klasserna kan samtalas av GC när minnet är lågt
- **SHA-256 hash**: Cache-nyckel baserat på källkod
- **Max cache-storlek**: 1000 klasser (auto-clear vid överskridning)

### Kompileringstid
- Första kompilering: ~100-150ms per klass
- Cached-klassning: < 1ms (hämtning från cache)

### Minnesutgift
- Per klass: ~50-100KB (beroende på fältantal)
- Cache med 1000 klasser: ~50-100MB

## Framtida Förbättringar

- [ ] Async-kompilering för latency > 100ms
- [ ] Redis-backed distributed cache
- [ ] Polymorfism & arv-stöd
- [ ] Default-värden för fält
- [ ] Collection-typstöd (List, Set, Map)
- [ ] Custom annotations support
- [ ] Groovy trait-stöd för mixin

## Felhantering

Felmeddelanden returneras med:
- **400 Bad Request**: Valideringsfel (ogiltiga namn, typer, etc.)
- **500 Internal Server Error**: Kompileringsfel, instansieringsfel, marshalling-fel

## Loggning

Alla operationer loggas på INFO/DEBUG-nivå:
- Klassgenering
- Kompilering
- Instansiering
- Cache-statistik
- Validering

Aktivera DEBUG-loggning i `application.properties`:
```properties
logging.level.com.example.dynamic=DEBUG
```

## Licens

Exempel för utbildning

## Kontakt

Denna implementering följer arkitektur för dynamisk klassgenering med Groovy.
