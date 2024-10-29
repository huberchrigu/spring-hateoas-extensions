package ch.chrigu.hateoas.server.reactive.data

import org.springframework.core.MethodParameter
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Pageable
import org.springframework.data.web.HateoasPageableHandlerMethodArgumentResolver
import org.springframework.data.web.isOneIndexedParameters
import org.springframework.hateoas.*
import org.springframework.hateoas.server.core.EmbeddedWrappers
import org.springframework.hateoas.server.reactive.ReactiveRepresentationModelAssembler
import org.springframework.lang.Nullable
import org.springframework.util.Assert
import org.springframework.web.server.ServerWebExchange
import org.springframework.web.util.UriComponents
import org.springframework.web.util.UriComponentsBuilder
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

/**
 * [RepresentationModelAssembler] to easily convert [Page] instances into [PagedModel].
 *
 * @author Christoph Huber
 */
class ReactivePagedResourcesAssembler<T : Any> private constructor(
    resolver: HateoasPageableHandlerMethodArgumentResolver?,
    private val baseUri: UriComponents?,
    private val parameter: MethodParameter?
) : ReactiveRepresentationModelAssembler<Page<T>, PagedModel<EntityModel<T>>> {
    private val pageableResolver: HateoasPageableHandlerMethodArgumentResolver = resolver ?: HateoasPageableHandlerMethodArgumentResolver()
    private val wrappers = EmbeddedWrappers(false)

    private var forceFirstAndLastRels = false

    /**
     * Creates a new [ReactivePagedResourcesAssembler] using the given [PageableHandlerMethodArgumentResolver] and
     * base URI. If the former is null, a default one will be created. If the latter is null, calls
     * to [.toModel] will use the current request's URI to build the relevant previous and next links.
     *
     * @param resolver can be null.
     * @param baseUri can be null.
     */
    constructor(resolver: HateoasPageableHandlerMethodArgumentResolver?, baseUri: UriComponents?) : this(resolver, baseUri, null)

    /**
     * Configures whether to always add `first` and `last` links to the [PagedModel] created. Defaults
     * to false which means that `first` and `last` links only appear in conjunction with
     * `prev` and `next` links.
     *
     * @param forceFirstAndLastRels whether to always add `first` and `last` links to the [PagedModel]
     * created.
     * @since 1.11
     */
    fun setForceFirstAndLastRels(forceFirstAndLastRels: Boolean) {
        this.forceFirstAndLastRels = forceFirstAndLastRels
    }

    /**
     * Creates a new [ReactivePagedResourcesAssembler] with the given reference [MethodParameter].
     *
     * @param parameter can be null.
     * @return will never be null.
     * @since 3.1
     */
    fun withParameter(@Nullable parameter: MethodParameter?): ReactivePagedResourcesAssembler<T> {
        return ReactivePagedResourcesAssembler(pageableResolver, baseUri, parameter)
    }

    override fun toModel(entity: Page<T>, exchange: ServerWebExchange): Mono<PagedModel<EntityModel<T>>> {
        return toModel(entity, { content, _ -> Mono.just(EntityModel.of(content)) }, exchange)
    }

    /**
     * Creates a new [PagedModel] by converting the given [Page] into a [PageMetadata] instance and
     * wrapping the contained elements into [PagedModel] instances. Will add pagination links based on the given the
     * self link.
     *
     * @param page must not be null.
     * @param selfLink must not be null.
     * @return
     */
    fun toModel(page: Page<T>, selfLink: Link, exchange: ServerWebExchange): Mono<PagedModel<EntityModel<T>>> {
        return toModel(page, { content, _ -> Mono.just(EntityModel.of(content)) }, selfLink, exchange)
    }

    /**
     * Creates a new [PagedModel] by converting the given [Page] into a [PageMetadata] instance and
     * using the given [PagedModel] to turn elements of the [Page] into resources.
     *
     * @param page must not be null.
     * @param assembler must not be null.
     */
    fun <R : EntityModel<out T>> toModel(page: Page<T>, assembler: ReactiveRepresentationModelAssembler<T, R>, exchange: ServerWebExchange): Mono<PagedModel<R>> {
        return createModel(page, assembler, null, exchange)
    }

    /**
     * Creates a new [PagedModel] by converting the given [Page] into a [PageMetadata] instance and
     * using the given [PagedModel] to turn elements of the [Page] into resources. Will add pagination links
     * based on the given the self link.
     *
     * @param page must not be null.
     * @param assembler must not be null.
     * @param link must not be null.
     * @return
     */
    fun <R : RepresentationModel<*>?> toModel(page: Page<T>, assembler: ReactiveRepresentationModelAssembler<T, R>, link: Link, exchange: ServerWebExchange): Mono<PagedModel<R>> {
        return createModel(page, assembler, link, exchange)
    }

    /**
     * Creates a [PagedModel] with an empt collection [EmbeddedWrapper] for the given domain type.
     *
     * @param page must not be null, content must be empty.
     * @param type must not be null.
     * @return
     * @since 2.0
     */
    fun toEmptyModel(page: Page<*>, type: Class<*>?, exchange: ServerWebExchange): PagedModel<*> {
        return toEmptyModel(page, type!!, null, exchange)
    }

    /**
     * Creates a [PagedModel] with an empt collection [EmbeddedWrapper] for the given domain type.
     *
     * @param page must not be null, content must be empty.
     * @param type must not be null.
     * @param link must not be null.
     * @return
     * @since 1.11
     */
    fun toEmptyModel(page: Page<*>, type: Class<*>, link: Link?, exchange: ServerWebExchange): PagedModel<*> {
        Assert.isTrue(!page.hasContent(), "Page must not have any content")

        val metadata = asPageMetadata(page)

        val wrapper = wrappers.emptyCollectionOf(type)
        val embedded = listOf(wrapper)

        return addPaginationLinks(PagedModel.of(embedded, metadata), page, link, exchange)
    }

    /**
     * Creates the [PagedModel] to be equipped with pagination links downstream.
     *
     * @param resources the original page's elements mapped into [RepresentationModel] instances.
     * @param metadata the calculated [PageMetadata], must not be null.
     * @param page the original page handed to the assembler, must not be null.
     * @return must not be null.
     */
    private fun <R : RepresentationModel<*>?, S> createPagedModel(resources: List<R>, metadata: PagedModel.PageMetadata?, page: Page<S>): PagedModel<R> {
        Assert.notNull(resources, "Content resources must not be null")
        Assert.notNull(metadata, "PageMetadata must not be null")
        Assert.notNull(page, "Page must not be null")

        return PagedModel.of(resources, metadata)
    }

    private fun <R : RepresentationModel<*>?> createModel(
        page: Page<T>,
        assembler: ReactiveRepresentationModelAssembler<T, R>,
        link: Link?,
        exchange: ServerWebExchange
    ): Mono<PagedModel<R>> {
        val resources = Flux.fromIterable(page).flatMap { assembler.toModel(it, exchange) }.collectList()
        val resource = resources.map { createPagedModel(it, asPageMetadata(page), page) }

        return resource.map { addPaginationLinks(it, page, link, exchange) }
    }

    private fun <R> addPaginationLinks(resources: PagedModel<R>, page: Page<*>, link: Link?, exchange: ServerWebExchange): PagedModel<R> {
        val base = getUriTemplate(link, exchange)

        val isNavigable = page.hasPrevious() || page.hasNext()

        if (isNavigable || forceFirstAndLastRels) {
            resources.add(createLink(base, PageRequest.of(0, page.size, page.sort), IanaLinkRelations.FIRST))
        }

        if (page.hasPrevious()) {
            resources.add(createLink(base, page.previousPageable(), IanaLinkRelations.PREV))
        }

        val selfLink = link?.withSelfRel() ?: createLink(base, page.pageable, IanaLinkRelations.SELF)

        resources.add(selfLink)

        if (page.hasNext()) {
            resources.add(createLink(base, page.nextPageable(), IanaLinkRelations.NEXT))
        }

        if (isNavigable || forceFirstAndLastRels) {
            val lastIndex = if (page.totalPages == 0) 0 else page.totalPages - 1

            resources
                .add(createLink(base, PageRequest.of(lastIndex, page.size, page.sort), IanaLinkRelations.LAST))
        }

        return resources
    }

    /**
     * Returns a default URI string either from the one configured on assembler creation or by looking it up from the
     * current request.
     *
     * @return
     */
    private fun getUriTemplate(baseLink: Link?, exchange: ServerWebExchange): UriTemplate {
        return UriTemplate.of(baseLink?.href ?: this.baseUriOrCurrentRequest(exchange))
    }

    /**
     * Creates a [Link] with the given [LinkRelation] that will be based on the given [UriTemplate] but
     * enriched with the values of the given [Pageable] (if not null).
     *
     * @param base must not be null.
     * @param pageable can be null
     * @param relation must not be null.
     * @return
     */
    private fun createLink(base: UriTemplate, pageable: Pageable, relation: LinkRelation): Link {
        val builder = UriComponentsBuilder.fromUri(base.expand())
        pageableResolver.enhance(builder, parameter, pageable)

        return Link.of(UriTemplate.of(builder.build().toString()), relation)
    }

    /**
     * Creates a new [PageMetadata] instance from the given [Page].
     *
     * @param page must not be null.
     * @return
     */
    private fun asPageMetadata(page: Page<*>): PagedModel.PageMetadata {
        val number: Int = if (pageableResolver.isOneIndexedParameters()) page.number + 1 else page.number

        return PagedModel.PageMetadata(page.size.toLong(), number.toLong(), page.totalElements, page.totalPages.toLong())
    }

    private fun baseUriOrCurrentRequest(exchange: ServerWebExchange): String {
        return baseUri?.let { obj: UriComponents -> obj.toString() } ?: currentRequest(exchange)
    }

    companion object {
        private fun currentRequest(exchange: ServerWebExchange): String {
            return exchange.request.uri.toString()
        }
    }
}
