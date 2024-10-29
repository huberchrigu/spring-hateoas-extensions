/*
 * Copyright 2013-2024 the original author or authors.
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
package ch.chrigu.hateoas.server.reactive.data

import org.springframework.core.MethodParameter
import org.springframework.data.web.HateoasPageableHandlerMethodArgumentResolver
import org.springframework.data.web.findMatchingPageableParameter
import org.springframework.lang.NonNull
import org.springframework.lang.Nullable
import org.springframework.web.bind.support.WebDataBinderFactory
import org.springframework.web.context.request.NativeWebRequest
import org.springframework.web.method.support.HandlerMethodArgumentResolver
import org.springframework.web.method.support.ModelAndViewContainer

/**
 * [HandlerMethodArgumentResolver] to allow injection of [ReactivePagedResourcesAssembler] into Spring Webflux
 * controller methods.
 *
 * @author Christoph Huber
 */
class ReactivePagedResourcesAssemblerArgumentResolver(private val resolver: HateoasPageableHandlerMethodArgumentResolver) : HandlerMethodArgumentResolver {

    override fun supportsParameter(parameter: MethodParameter): Boolean {
        return ReactivePagedResourcesAssembler::class.java == parameter.parameterType
    }

    @NonNull
    override fun resolveArgument(
        parameter: MethodParameter, @Nullable mavContainer: ModelAndViewContainer?,
        webRequest: NativeWebRequest, @Nullable binderFactory: WebDataBinderFactory?
    ): Any {
        return ReactivePagedResourcesAssembler<Any>(resolver, null).withParameter(findMatchingPageableParameter(parameter))
    }
}
