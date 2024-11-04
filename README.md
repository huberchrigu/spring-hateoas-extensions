# How to use

Add this dependency to your Maven `pom.xml`:
```xml
<dependency>
    <groupId>tech.chrigu.hateoas</groupId>
    <artifactId>spring-hateoas-extensions</artifactId>
    <version>0.0.1</version>
</dependency>
```

# Reactive Kotlin extensions/DSL
```kotlin
val link = linkTo<CustomerController> { findById("15") } withRel SELF

val customer = EntityModel.of(Customer("15", "John Doe"))
    .add(CustomerController::class) { entity ->
        linkTo { findById(entity.content!!.id) } withRel SELF
        linkTo { findProductsById(entity.content!!.id) } withRel REL_PRODUCTS
    }
// or
val customer = Mono.just(CustomerModel("15", "John Doe"))
    .add { linkTo<CustomerController> { findById(it.id) } withRel SELF }
```

## With coroutines
```kotlin
val link = linkTo<CustomerController> { findById("15") } awaitRel SELF
```

# Model Assembler

## ReactiveEntityModelAssembler

The `SimpleReactiveModelAssembler` can be hard to use, because its `addLinks()` functions do not work with reactive types. Hence, if the `linkTo()` link builder method is used, the model assembler looks like this:

```kotlin
class ObjectModelAssembler : SimpleReactiveRepresentationModelAssembler<Object> {
    override fun toCollectionModel(entities: Flux<out Object>, exchange: ServerWebExchange): Mono<CollectionModel<EntityModel<Object>>> {
        return super.toCollectionModel(entities, exchange)
            .zipWith(getCollectionLink(exchange)) { model, link -> model.add(link) }
    }
// ...
}
```

With this module, this can be simplified to

```kotlin
class ObjectModelAssembler : ReactiveEntityModelAssembler<Object> {
    override fun getLinks(resources: CollectionModel<EntityModel<Object>>, exchange: ServerWebExchange) = Flux.concat(getCollectionLink(exchange))
// ...
}
```

## ReactivePagedResourcesAssembler

If you need converting Spring Data's `Page` into `PagedModels`, you can register a bean of type `ReactivePagedResourcesAssemblerArgumentResolver`. This will enable controller methods like this:

```kotlin
    @GetMapping
    fun getContracts(
        pageable: Pageable,
        pagedResourcesAssembler: ReactivePagedResourcesAssembler<ContractBaseProperties>,
        exchange: ServerWebExchange
    ): Mono<PagedModel<EntityModel<ContractBaseProperties>>> {
        return contractService.getContracts(pageable)
            .flatMap { pagedResourcesAssembler.toModel(it, contractModelAssembler, exchange) }
    }
```

## SimpleCoroutineRepresentationModelAssembler
This module contains a coroutine alternative for a `RepresentationModelAssembler`, the `CoroutineRepresentationModelAssembler`. Likewise, there is the `SimpleCoroutineRepresentationModelAssembler` alternative that can be used like this:

```kotlin
class ResourceAssemblerWithCustomLink : SimpleCoroutineRepresentationModelAssembler<Employee> {
    override suspend fun addLinks(resource: EntityModel<Employee>, exchange: ServerWebExchange): EntityModel<Employee> {
        return resource.add(Link.of("/employees").withRel("employees"))
    }
}
```

# Release to Maven Central
To make `mvn deploy` work, your settings.xml should look as follows:
```xml
<?xml version="1.0" encoding="UTF-8"?>
<settings xmlns="https://maven.apache.org/SETTINGS/1.0.0"
          xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
          xsi:schemaLocation="https://maven.apache.org/SETTINGS/1.0.0 http://maven.apache.org/xsd/settings-1.0.0.xsd">
    <servers>
        <server>
            <id>central</id>
            <username>see https://central.sonatype.com/account</username>
            <password>see https://central.sonatype.com/account</password>
        </server>
    </servers>

    <profiles>
        <profile>
            <id>ossrh</id>
            <activation>
                <activeByDefault>true</activeByDefault>
            </activation>
            <properties>
                <gpg.executable>gpg</gpg.executable>
                <gpg.keyname>see gpg --list-keys and gpg --keyserver keyserver.ubuntu.com --send-keys [KEY_NAME]</gpg.keyname>
                <gpg.passphrase>see gpg --gen-key</gpg.passphrase>
            </properties>
        </profile>
    </profiles>
</settings>
```
