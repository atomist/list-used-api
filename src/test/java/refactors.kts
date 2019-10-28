@file:Include("base.kts")

import com.github.javaparser.ast.CompilationUnit
import com.github.javaparser.ast.expr.MethodCallExpr
import com.github.javaparser.ast.stmt.ExpressionStmt
import com.github.javaparser.printer.PrettyPrinterConfiguration
import java.io.File

fun MethodCallExpr.getQualifiedSignature(): String? {
    try {
        val resolved = this.resolve();
        return resolved.qualifiedSignature;
    } catch(e: Exception) {
        return null;
    }
}

fun replaceMethodOnSameClass(qualifiedName: String, replacement: String): (CompilationUnit, File) -> Unit {
    return { cu, file ->
        run {
            var changed = false
            cu.findAll(ExpressionStmt::class.java)
                    ?.filter { it.childNodes.count() == 1 && it.childNodes.get(0) is MethodCallExpr}
                    ?.forEach { mce ->
                        run {
                            mce.setName(replacement)
                            changed = true;
                        }
                    }
            if (changed) {
                file.writeText(cu.toString(PrettyPrinterConfiguration()))
            }
        }
    }
}
