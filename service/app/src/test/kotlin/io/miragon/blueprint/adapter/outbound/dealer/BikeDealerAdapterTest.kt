package io.miragon.blueprint.adapter.outbound.dealer

import io.miragon.blueprint.domain.bike.BikeId
import io.miragon.blueprint.domain.bike.OrderId
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatCode
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test

class BikeDealerAdapterTest {

    private val underTest = BikeDealerAdapter()

    @Test
    fun `a bike outside the out-of-stock list is available`() {
        // given: a regularly stocked bike / when-then: the dealer reports it as available
        assertThat(underTest.checkAvailability(BikeId("BIKE-900"))).isTrue()
    }

    @Test
    fun `the out-of-stock demo bike is unavailable`() {
        // given: the sentinel out-of-stock bike / when-then: the dealer reports it as unavailable
        assertThat(underTest.checkAvailability(BikeId("BIKE-OOS"))).isFalse()
    }

    @Test
    fun `order mints a fresh, unique order id`() {
        // given: the same bike ordered twice
        val first = underTest.order(BikeId("BIKE-900"))
        val second = underTest.order(BikeId("BIKE-900"))
        // then: each order gets its own opaque reference, independent of the bike id
        assertThat(first.value).startsWith("ORDER-")
        assertThat(first).isNotEqualTo(second)
    }

    @Test
    fun `the incident-demo bike stays available so the flow reaches the order step`() {
        // given: the poison bike used by the incident demo / when-then: it is not on the out-of-stock
        // list, so the process reaches serviceTask_orderBike (where the dealer then fails) instead of
        // taking the out-of-stock branch
        assertThat(underTest.checkAvailability(BikeId("BIKE-FAIL"))).isTrue()
    }

    @Test
    fun `ordering the incident-demo bike fails as a simulated dealer outage`() {
        // given: the poison bike / when-then: the dealer call throws so the external task fails → incident
        assertThatThrownBy { underTest.order(BikeId("BIKE-FAIL")) }
            .isInstanceOf(BikeDealerAdapter.DealerUnavailableException::class.java)
    }

    @Test
    fun `requestCancellation reports the order as cancellable`() {
        // given: a placed order / when-then: the demo dealer always allows cancellation
        assertThat(underTest.requestCancellation(OrderId("ORDER-900"))).isTrue()
    }

    @Test
    fun `bookCancellationCosts runs without error`() {
        // given: a placed order / when-then: booking the dealer's costs runs without error
        assertThatCode { underTest.bookCancellationCosts(OrderId("ORDER-900")) }.doesNotThrowAnyException()
    }
}
