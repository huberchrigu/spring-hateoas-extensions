package ch.chrigu.hateoas.server.reactive

import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.toList
import org.springframework.hateoas.CollectionModel
import org.springframework.hateoas.RepresentationModel
import org.springframework.web.server.ServerWebExchange

/**
 * Coroutine variant of [RepresentationModelAssembler].
 *
 * @author Christoph Huber
 */
interface CoroutineRepresentationModelAssembler<T, D : RepresentationModel<*>> {
    /**
     * Converts the given entity into a `D`, which extends [RepresentationModel].
     */
    suspend fun toModel(entity: T, exchange: ServerWebExchange): D

    /**
     * Converts an [Iterable] or `T`s into an [Iterable] of [RepresentationModel] and wraps them
     * in a [CollectionModel] instance.
     */
    suspend fun toCollectionModel(entities: kotlinx.coroutines.flow.Flow<T>, exchange: ServerWebExchange): CollectionModel<D> {
        val entities = entities.map { toModel(it, exchange) }.toList()
        return CollectionModel.of(entities)
    }
}
