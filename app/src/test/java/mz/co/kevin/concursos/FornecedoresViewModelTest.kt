package mz.co.kevin.concursos

import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import mz.co.kevin.concursos.data.model.FornecedorCef
import mz.co.kevin.concursos.data.repository.UfsaRepository
import mz.co.kevin.concursos.ui.screens.FornecedoresViewModel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test

class FornecedoresViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val fornecedoresFlow = MutableStateFlow<List<FornecedorCef>>(emptyList())
    private val provinciasFlow = MutableStateFlow<List<String>>(emptyList())

    private val repo = mockk<UfsaRepository>(relaxed = true) {
        every { observarFornecedores(any(), any()) } returns fornecedoresFlow
        every { observarProvinciasFornecedores() } returns provinciasFlow
    }

    private fun vm() = FornecedoresViewModel(repo = repo, resolveString = { "erro" })

    @Test fun `estado inicial esta limpo`() {
        val state = vm().state.value
        assertEquals("", state.provinciaSelecionada)
        assertEquals("", state.termoPesquisa)
        assertEquals(0, state.filtrosActivos)
        assertNull(state.erro)
    }

    @Test fun `alterarPesquisa actualiza termoPesquisa`() {
        val vm = vm()
        vm.alterarPesquisa("construção")
        assertEquals("construção", vm.state.value.termoPesquisa)
    }

    @Test fun `alterarProvincia actualiza province e conta filtro`() {
        val vm = vm()
        vm.alterarProvincia("Maputo")
        assertEquals("Maputo", vm.state.value.provinciaSelecionada)
        assertEquals(1, vm.state.value.filtrosActivos)
    }

    @Test fun `limparFiltros repoe estado inicial`() {
        val vm = vm()
        vm.alterarProvincia("Gaza")
        vm.alterarAno("2024")
        vm.limparFiltros()
        val state = vm.state.value
        assertEquals("", state.provinciaSelecionada)
        assertEquals("", state.anoInscricao)
        assertEquals(0, state.filtrosActivos)
    }

    @Test fun `pesquisarRemoto invoca sincronizarFornecedores no repo`() = runTest {
        coEvery { repo.sincronizarFornecedores(any(), any()) } returns Unit
        val vm = vm()
        vm.pesquisarRemoto()
        coVerify { repo.sincronizarFornecedores(any(), any()) }
    }

    @Test fun `pesquisarRemoto coloca erro no estado quando repo falha`() = runTest {
        coEvery { repo.sincronizarFornecedores(any(), any()) } throws RuntimeException("timeout")
        val vm = vm()
        vm.pesquisarRemoto()
        assertEquals("timeout", vm.state.value.erro)
    }

    @Test fun `pesquisarRemoto usa resolveString quando mensagem nula`() = runTest {
        coEvery { repo.sincronizarFornecedores(any(), any()) } throws RuntimeException()
        val vm = FornecedoresViewModel(repo = repo, resolveString = { "erro_generico" })
        vm.pesquisarRemoto()
        assertEquals("erro_generico", vm.state.value.erro)
    }
}
