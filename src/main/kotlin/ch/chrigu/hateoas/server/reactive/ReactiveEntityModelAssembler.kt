/*
 * Copyright 2019-2024 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package ch.chrigu.hateoas.server.reactive

import org.springframework.hateoas.CollectionModel
import org.springframework.hateoas.EntityModel
import org.springframework.hateoas.Link
import org.springframework.hateoas.server.reactive.ReactiveRepresentationModelAssembler
import org.springframework.web.server.ServerWebExchange
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

/**
 * Reactive variant of [RepresentationModelAssembler] combined with [SimpleRepresentationModelAssembler].
 *
 * @author Christoph Huber
 */
interface ReactiveEntityModelAssembler<T : Any> : ReactiveRepresentationModelAssembler<T, EntityModel<T>> {
    /**
     * Converts the given entity into a [EntityModel] wrapped in a [Mono].
     *
     * @param entity must not be null.
     * @param exchange must not be null.
     * @return will never be null.
     */
    override fun toModel(entity: T, exchange: ServerWebExchange): Mono<EntityModel<T>> {
        val resource = EntityModel.of(entity)
        return getLinks(resource, exchange).collectList()
            .map { resource.add(it) }
    }

    /**
     * Define links to add to every individual [EntityModel].
     *
     * @param resource must not be null.
     * @param exchange must not be null.
     * @return will never be null.
     */
    fun getLinks(resource: EntityModel<T>, exchange: ServerWebExchange): Flux<Link> {
        return Flux.empty()
    }

    /**
     * Converts all given entities into resources and wraps the collection as a resource as well.
     *
     * @see .toModel
     * @param entities must not be null.
     * @return [CollectionModel] containing [EntityModel] of `T`, will never be null..
     */
    override fun toCollectionModel(
        entities: Flux<out T>,
        exchange: ServerWebExchange
    ): Mono<CollectionModel<EntityModel<T>>> {
        return entities
            .flatMap { toModel(it, exchange) }
            .collectList()
            .map { CollectionModel.of(it) }
            .flatMap { resources -> getLinks(resources, exchange).collectList().map { resources.add(it) } }
    }

    /**
     * Define links to add to the [CollectionModel] collection.
     *
     * @param resources must not be null.
     * @return will never be null.
     */
    fun getLinks(resources: CollectionModel<EntityModel<T>>, exchange: ServerWebExchange): Flux<Link> {
        return Flux.empty()
    }
}
