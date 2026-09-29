package com.br.semesperanca.backendleads.domain.repository

import com.br.semesperanca.backendleads.domain.model.Property
import com.br.semesperanca.backendleads.support.AbstractRepositoryTest
import com.br.semesperanca.backendleads.support.TestFixtures
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.data.jpa.domain.Specification
import java.math.BigDecimal
import java.util.UUID

/**
 * `Property` concentra as duas queries derivadas do projeto e o filtro mais
 * rico — sete critérios encadeados com `Specification`. Aqui eles rodam contra
 * o SQL gerado de verdade, e não contra um `CriteriaBuilder` mockado, então o
 * que o teste pega é erro de nome de coluna, de tipo e de comparação.
 */
class PropertyRepositoryTest : AbstractRepositoryTest() {
    @Autowired
    private lateinit var repository: PropertyRepository

    private lateinit var agencyId: UUID

    private lateinit var outraAgencyId: UUID

    @BeforeEach
    fun setUp() {
        agencyId = insertAgency("Imob A")
        outraAgencyId = insertAgency("Imob B")
    }

    private fun novo(
        agency: UUID = agencyId,
        title: String = "Apartamento",
        status: String? = "available",
        active: Boolean = true,
        city: String? = "Sao Paulo",
        price: BigDecimal? = BigDecimal("350000.00"),
        bedrooms: Int? = 2,
    ): Property =
        TestFixtures.property(
            id = null,
            realEstateAgencyId = agency,
            title = title,
            status = status,
            active = active,
            city = city,
            price = price,
            bedrooms = bedrooms,
            createdAt = null,
            updatedAt = null,
        )

    @Test
    fun `save preenche o id e o prePersist do createdAt`() {
        val saved = repository.save(novo())

        assertThat(saved.id).isNotNull()
        assertThat(saved.createdAt).isNotNull()
    }

    @Test
    fun `findByRealEstateAgencyId devolve só os imóveis da agência`() {
        repository.save(novo(title = "A1"))
        repository.save(novo(title = "A2"))
        repository.save(novo(agency = outraAgencyId, title = "B1"))

        val daAgencia = repository.findByRealEstateAgencyId(agencyId)

        assertThat(daAgencia.map { it.title }).containsExactlyInAnyOrder("A1", "A2")
    }

    @Test
    fun `findByRealEstateAgencyId de agência sem imóveis devolve lista vazia`() {
        assertThat(repository.findByRealEstateAgencyId(UUID.randomUUID())).isEmpty()
    }

    @Test
    fun `findByTitleContainingIgnoreCase acha ignorando maiúsculas e minúsculas`() {
        repository.save(novo(title = "Cobertura Duplex"))

        assertThat(repository.findByTitleContainingIgnoreCase("cobertura")).isPresent
        assertThat(repository.findByTitleContainingIgnoreCase("DUPLEX")).isPresent
    }

    @Test
    fun `findByTitleContainingIgnoreCase com trecho ausente devolve vazio`() {
        repository.save(novo(title = "Cobertura Duplex"))

        assertThat(repository.findByTitleContainingIgnoreCase("terreno")).isEmpty
    }

    @Test
    fun `Specification por status e agency compõe os dois predicados`() {
        repository.save(novo(title = "disponivel", status = "available"))
        repository.save(novo(title = "vendido", status = "sold"))
        repository.save(novo(agency = outraAgencyId, title = "de-outra", status = "available"))

        val spec = statusSpec("available").and(agencySpec(agencyId))

        assertThat(repository.findAll(spec).map { it.title }).containsExactly("disponivel")
    }

    @Test
    fun `Specification com faixa de preço e número de quartos`() {
        repository.save(novo(title = "barato-1-quarto", price = BigDecimal("150000.00"), bedrooms = 1))
        repository.save(novo(title = "no-limite", price = BigDecimal("350000.00"), bedrooms = 2))
        repository.save(novo(title = "no-limite-sem-quarto", price = BigDecimal("350000.00"), bedrooms = null))
        repository.save(novo(title = "caro", price = BigDecimal("900000.00"), bedrooms = 4))

        val spec = priceBetween(BigDecimal("100000.00"), BigDecimal("500000.00")).and(minBedrooms(2))

        assertThat(repository.findAll(spec).map { it.title }).containsExactly("no-limite")
    }

    @Test
    fun `Specification com busca textual e paginação respeita size e offset`() {
        repeat(5) { repository.save(novo(title = "Imovel $it", city = "Campinas")) }
        repository.save(novo(title = "Imovel distante", city = "Recife"))

        val spec =
            Specification<Property> { root, _, cb ->
                cb.like(cb.lower(root.get<String>("city")), "%campinas%")
            }

        val pagina = repository.findAll(spec, PageRequest.of(1, 2, Sort.by(Sort.Direction.ASC, "title")))

        assertThat(pagina.content.map { it.title }).containsExactly("Imovel 2", "Imovel 3")
        assertThat(pagina.totalElements).isEqualTo(5)
        assertThat(pagina.totalPages).isEqualTo(3)
    }

    @Test
    fun `filtro por active falso esconde os imóveis desligados`() {
        repository.save(novo(title = "ativo", active = true))
        repository.save(novo(title = "inativo", active = false))

        val spec = Specification<Property> { root, _, cb -> cb.isFalse(root.get("active")) }

        assertThat(repository.findAll(spec).map { it.title }).containsExactly("inativo")
    }

    @Test
    fun `deleteById remove o imóvel`() {
        val saved = repository.save(novo())

        repository.deleteById(saved.id!!)

        assertThat(repository.existsById(saved.id!!)).isFalse()
    }

    private fun statusSpec(status: String): Specification<Property> =
        Specification { root, _, cb -> cb.equal(root.get<String>("status"), status) }

    private fun agencySpec(agency: UUID): Specification<Property> =
        Specification { root, _, cb -> cb.equal(root.get<UUID>("realEstateAgencyId"), agency) }

    private fun priceBetween(
        de: BigDecimal,
        ate: BigDecimal,
    ): Specification<Property> = Specification { root, _, cb -> cb.between(root.get<BigDecimal>("price"), de, ate) }

    private fun minBedrooms(min: Int): Specification<Property> =
        Specification { root, _, cb -> cb.greaterThanOrEqualTo(root.get<Int>("bedrooms"), min) }
}
