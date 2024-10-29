package org.springframework.data.web

import org.springframework.core.MethodParameter

fun findMatchingPageableParameter(parameter: MethodParameter): MethodParameter? = PageableMethodParameterUtils.findMatchingPageableParameter(parameter)
