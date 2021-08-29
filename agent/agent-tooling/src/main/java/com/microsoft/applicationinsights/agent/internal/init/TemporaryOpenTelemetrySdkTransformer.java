/*
 * ApplicationInsights-Java
 * Copyright (c) Microsoft Corporation
 * All rights reserved.
 *
 * MIT License
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this
 * software and associated documentation files (the ""Software""), to deal in the Software
 * without restriction, including without limitation the rights to use, copy, modify, merge,
 * publish, distribute, sublicense, and/or sell copies of the Software, and to permit
 * persons to whom the Software is furnished to do so, subject to the following conditions:
 * The above copyright notice and this permission notice shall be included in all copies or
 * substantial portions of the Software.
 * THE SOFTWARE IS PROVIDED *AS IS*, WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED,
 * INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR
 * PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE
 * FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR
 * OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER
 * DEALINGS IN THE SOFTWARE.
 */

package com.microsoft.applicationinsights.agent.internal.init;

import static net.bytebuddy.jar.asm.Opcodes.ASM7;

import java.lang.instrument.ClassFileTransformer;
import java.security.ProtectionDomain;
import net.bytebuddy.jar.asm.ClassReader;
import net.bytebuddy.jar.asm.ClassVisitor;
import net.bytebuddy.jar.asm.ClassWriter;
import net.bytebuddy.jar.asm.MethodVisitor;

// temporary until OpenTelemetry SDK 1.6.0 is release
// which contains https://github.com/open-telemetry/opentelemetry-java/pull/3564
public class TemporaryOpenTelemetrySdkTransformer implements ClassFileTransformer {

  @Override
  @SuppressWarnings("SystemOut")
  public byte[] transform(
      ClassLoader loader,
      String className,
      Class<?> classBeingRedefined,
      ProtectionDomain protectionDomain,
      byte[] classfileBuffer) {
    if (!className.equals("io/opentelemetry/sdk/trace/SdkSpanBuilder")) {
      return null;
    }
    if (loader == null || !loader.getClass().getName().startsWith("io.opentelemetry.javaagent")) {
      return null;
    }
    try {
      ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_MAXS);
      ClassVisitor cv = new SdkSpanBuilderClassVisitor(cw);
      ClassReader cr = new ClassReader(classfileBuffer);
      cr.accept(cv, 0);
      return cw.toByteArray();
    } catch (Throwable t) {
      // logging hasn't been initialized yet
      t.printStackTrace();
      return null;
    }
  }

  private static class SdkSpanBuilderClassVisitor extends ClassVisitor {

    private final ClassWriter cw;

    private SdkSpanBuilderClassVisitor(ClassWriter cw) {
      super(ASM7, cw);
      this.cw = cw;
    }

    @Override
    public MethodVisitor visitMethod(
        int access, String name, String descriptor, String signature, String[] exceptions) {
      MethodVisitor mv = cw.visitMethod(access, name, descriptor, signature, exceptions);
      if (name.equals("startSpan")) {
        return new StartSpanMethodVisitor(mv);
      } else {
        return mv;
      }
    }
  }

  private static class StartSpanMethodVisitor extends MethodVisitor {

    private StartSpanMethodVisitor(MethodVisitor mv) {
      super(ASM7, mv);
    }

    @Override
    public void visitMethodInsn(
        int opcode, String owner, String name, String descriptor, boolean isInterface) {

      if (owner.equals("io/opentelemetry/api/trace/SpanContext") && name.equals("create")) {
        super.visitMethodInsn(
            opcode,
            "io/opentelemetry/sdk/trace/ImmutableValidSpanContext",
            "createBypassingValidation",
            descriptor,
            false);
      } else {
        super.visitMethodInsn(opcode, owner, name, descriptor, isInterface);
      }
    }
  }
}
