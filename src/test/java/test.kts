#!/usr/bin/env kscript
val transformer = replaceMethodOnSameClass("com.google.common.base.Converter.apply(A)", "convert")
ExecuteTransformCommand(transformer).main(args)
