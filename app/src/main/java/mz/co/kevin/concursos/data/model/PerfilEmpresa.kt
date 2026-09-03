package mz.co.kevin.concursos.data.model

import androidx.annotation.StringRes
import mz.co.kevin.concursos.R

enum class PorteEmpresa(@StringRes val labelRes: Int) {
    MICRO_PEQUENA(R.string.porte_micro_pequena),
    MEDIA(R.string.porte_media),
    GRANDE(R.string.porte_grande)
}

data class PerfilEmpresa(
    val nome: String = "",
    val nuit: String = "",
    val porte: PorteEmpresa = PorteEmpresa.MEDIA,
    val areasAtuacao: List<String> = emptyList(),
    val especialidadesTexto: String = "",
    val provinciasAtuacao: List<String> = listOf("Todas as Províncias"),
    val documentosDisponiveis: List<String> = emptyList(),
    val outrosDocumentos: String = "",
    val configurado: Boolean = false
) {
    companion object {
        val DOCUMENTOS_PADRAO = listOf(
            "Certificado de Inscrição no CEF (UFSA)",
            "Alvará / Licença Comercial válida",
            "Certidão de Quitação Fiscal (DGI / Finanças)",
            "Certidão de Quitação do INSS",
            "Certidão de Registo Comercial e Estatutos (BR)",
            "Demonstrações Financeiras / Balanço dos últimos exercícios",
            "Atestados de Capacidade Técnica / Declarações de boa execução",
            "Declaração Bancária / Linha de Crédito"
        )

        val AREAS_PADRAO = listOf(
            "Tecnologias de Informação, Software e Redes",
            "Equipamentos Informáticos e Consumíveis",
            "Telecomunicações e Fibra Óptica",
            "Construção Civil e Obras Públicas",
            "Manutenção e Reabilitação de Edifícios",
            "Fornecimento de Bens e Material de Escritório",
            "Consultoria, Estudos Técnicos e Auditoria",
            "Serviços de Limpeza, Higienização e Tratamento de Resíduos",
            "Segurança Privada e Vigilância",
            "Transportes, Logística e Aluguer de Viaturas",
            "Saúde, Medicamentos e Equipamento Hospitalar",
            "Hotelaria, Restauração e Serviços de Catering",
            "Formação Profissional e Capacitação"
        )

        val PROVINCIAS_MOCAMBIQUE = listOf(
            "Todas as Províncias",
            "Maputo Cidade",
            "Maputo Província",
            "Gaza",
            "Inhambane",
            "Sofala",
            "Manica",
            "Tete",
            "Zambézia",
            "Nampula",
            "Cabo Delgado",
            "Niassa"
        )
    }
}
