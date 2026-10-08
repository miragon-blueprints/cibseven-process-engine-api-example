package io.miragon.blueprint.adapter.inbound.rest

import com.ninjasquad.springmockk.MockkBean
import io.miragon.blueprint.application.port.inbound.SignContractUseCase
import io.miragon.blueprint.domain.leasing.ApplicationId
import io.mockk.Runs
import io.mockk.confirmVerified
import io.mockk.every
import io.mockk.just
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post

@WebMvcTest(SignContractController::class)
class SignContractControllerTest {

    @Autowired
    private lateinit var mockMvc: MockMvc

    @MockkBean
    private lateinit var useCase: SignContractUseCase

    @Test
    fun `customer signs the contract`() {

        // given: a valid application-id path variable & rest-operation
        val pathVar = "123e4567-e89b-12d3-a456-426614174000"
        every { useCase.signContract(any()) } just Runs
        val operation = post("/api/bike-leasing/{applicationId}/sign-contract", pathVar)

        // when: the request is performed
        val response = mockMvc.perform(operation).andReturn()

        // then: the use case is invoked and the response is 202 Accepted
        assertThat(response.response.status).isEqualTo(202)
        verify { useCase.signContract(ApplicationId.of(pathVar)) }
        confirmVerified(useCase)
    }
}
