package ch.chrigu.hateoas.server.reactive

import org.assertj.core.api.Assertions
import org.junit.jupiter.api.Test
import org.springframework.hateoas.EntityModel
import org.springframework.hateoas.Link
import org.springframework.mock.http.server.reactive.MockServerHttpRequest
import org.springframework.mock.web.server.MockServerWebExchange
import org.springframework.web.server.ServerWebExchange

/**
 * @author Christoph Huber
 */
internal class SimpleCoroutineRepresentationModelAssemblerTest {
    private val request = MockServerHttpRequest.get("http://localhost:8080/api")
    private val exchange = MockServerWebExchange.from(request)

    @Test
    fun convertingToResourceShouldWork() {
        kotlinx.coroutines.runBlocking {
            val assembler = TestResourceAssembler()
            val resource = assembler.toModel(Employee("Frodo"), exchange)

            Assertions.assertThat(resource.content!!.name).isEqualTo("Frodo")
            Assertions.assertThat(resource.links).isEmpty()
        }
    }

    @Test
    fun convertingToResourcesShouldWork() {
        kotlinx.coroutines.runBlocking {
            val assembler = TestResourceAssembler()
            val resources = assembler.toCollectionModel(kotlinx.coroutines.flow.flowOf(Employee("Frodo")), exchange)

            Assertions.assertThat(resources.content).containsExactly(EntityModel.of(Employee("Frodo")))
            Assertions.assertThat(resources.links).isEmpty()
        }
    }

    @Test
    fun convertingToResourceWithCustomLinksShouldWork() {
        kotlinx.coroutines.runBlocking {
            val assembler = ResourceAssemblerWithCustomLink()
            val resource = assembler.toModel(Employee("Frodo"), exchange)

            Assertions.assertThat(resource.content!!.name).isEqualTo("Frodo")
            Assertions.assertThat(resource.links).containsExactly(Link.of("/employees").withRel("employees"))
        }
    }

    @Test
    fun convertingToResourcesWithCustomLinksShouldWork() {
        kotlinx.coroutines.runBlocking {
            val assembler = ResourceAssemblerWithCustomLink()
            val resources = assembler.toCollectionModel(kotlinx.coroutines.flow.flowOf(Employee("Frodo")), exchange)

            Assertions.assertThat(resources.content).containsExactly(
                EntityModel.of(Employee("Frodo"), Link.of("/employees").withRel("employees"))
            )
            Assertions.assertThat(resources.links).isEmpty()
        }
    }

    internal inner class TestResourceAssembler : SimpleCoroutineRepresentationModelAssembler<Employee>

    internal inner class ResourceAssemblerWithCustomLink : SimpleCoroutineRepresentationModelAssembler<Employee> {
        override suspend fun addLinks(resource: EntityModel<Employee>, exchange: ServerWebExchange): EntityModel<Employee> {
            return resource.add(Link.of("/employees").withRel("employees"))
        }
    }

    data class Employee(val name: String)
}
