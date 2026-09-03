package mz.co.kevin.concursos.data.model

import androidx.annotation.StringRes
import mz.co.kevin.concursos.R

enum class CategoriaConcurso(@StringRes val labelRes: Int) {
    ABERTO(R.string.categoria_abertos),
    ADJUDICADO(R.string.categoria_adjudicados),
    CANCELADO(R.string.categoria_cancelados)
}
