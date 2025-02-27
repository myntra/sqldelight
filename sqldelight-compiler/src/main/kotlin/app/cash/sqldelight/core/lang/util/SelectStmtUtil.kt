package app.cash.sqldelight.core.lang.util

import app.cash.sqldelight.core.lang.util.TableNameElement.CreateTableName
import com.alecstrong.sql.psi.core.psi.SqlCompoundSelectStmt
import com.alecstrong.sql.psi.core.psi.SqlCreateTableStmt
import com.alecstrong.sql.psi.core.psi.SqlCreateViewStmt
import com.alecstrong.sql.psi.core.psi.SqlCreateVirtualTableStmt
import com.alecstrong.sql.psi.core.psi.SqlCteTableName
import com.alecstrong.sql.psi.core.psi.SqlNewTableName
import com.alecstrong.sql.psi.core.psi.SqlTableAlias
import com.alecstrong.sql.psi.core.psi.SqlTableName
import com.alecstrong.sql.psi.core.psi.SqlViewName
import com.alecstrong.sql.psi.core.psi.SqlWithClause
import com.intellij.psi.PsiElement

internal sealed class TableNameElement {
  abstract val name: String

  data class CreateTableName(private val tableName: SqlTableName) : TableNameElement() {
    override val name get() = tableName.name
  }

  data class NewTableName(private val newTableName: SqlNewTableName) : TableNameElement() {
    override val name get() = newTableName.name
  }
}

internal fun SqlCompoundSelectStmt.tablesObserved() = findChildrenOfType<SqlTableName>()
  .mapNotNull { it.reference?.resolve() }
  .distinct()
  .flatMap { it.referencedTables(this) }
  .distinctBy { it.name }

internal fun PsiElement.referencedTables(
  compoundSelectStmt: SqlCompoundSelectStmt? = null
): List<TableNameElement> = when (this) {
  is SqlCompoundSelectStmt -> tablesObserved()
  is SqlTableAlias -> source().referencedTables()
  is SqlNewTableName -> {
    listOf(TableNameElement.NewTableName(this))
  }
  is SqlTableName, is SqlViewName -> {
    when (val parentRule = parent!!) {
      is SqlCreateTableStmt -> listOf(CreateTableName(parentRule.tableName))
      is SqlCreateVirtualTableStmt -> listOf(CreateTableName(parentRule.tableName))
      is SqlCreateViewStmt -> parentRule.compoundSelectStmt?.tablesObserved().orEmpty()
      is SqlCteTableName -> {
        val withClause = parentRule.parent as SqlWithClause
        val index = withClause.cteTableNameList.indexOf(parentRule)
        val withSelect = withClause.withClauseAuxiliaryStmtList[index]
        if (withSelect.findChildOfType<SqlCompoundSelectStmt>() == compoundSelectStmt) {
          // Recursive subquery. We've already resolved the other tables in this recursive query
          // so quit out.
          emptyList()
        } else {
          withClause.withClauseAuxiliaryStmtList[index]
            .findChildOfType<SqlCompoundSelectStmt>()?.tablesObserved().orEmpty()
        }
      }
      else -> reference?.resolve()?.referencedTables().orEmpty()
    }
  }
  else -> throw IllegalStateException("Cannot get reference table for psi type ${this.javaClass}")
}
