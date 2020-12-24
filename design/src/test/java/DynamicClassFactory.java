import appbox.utils.StringUtil;
import org.objectweb.asm.*;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.HashMap;
import java.util.Map;

import static org.objectweb.asm.Opcodes.*;

/**
 * 动态类 工厂类
 */
public class DynamicClassFactory {
    /**
     * class的缓存
     */
    private static Map<String, byte[]> classPool = new HashMap<>();
    //查看class类结构: javap -verbose Hello.class
    public static byte[] getClassByte(String className) {
        String fullName="com/model/"+className;

        byte[] code=classPool.get(className);
        if(code==null){
            ClassWriter cw = new ClassWriter(0);
            cw.visit(V13, ACC_PUBLIC, fullName, null, "appbox/data/Entity",null);
            //生成带参数构造方法
            MethodVisitor constructor = cw.visitMethod(ACC_PUBLIC, "<init>", "(J)V", null, null);
            constructor.visitVarInsn(ALOAD, 0);//this
            constructor.visitVarInsn(LLOAD, 1);//this
            constructor.visitMethodInsn(INVOKESPECIAL, "appbox/data/Entity", "<init>", "(J)V",false);
            constructor.visitInsn(RETURN);
            constructor.visitMaxs(5, 5);
            constructor.visitEnd();

            //重载方法：writeMember
            MethodVisitor writeMember = cw.visitMethod(ACC_PUBLIC, "writeMember", "(SLappbox/serialization/IEntityMemberWriter;B)V", null, new String[]{"java/lang/Exception"});
            writeMember.visitInsn(RETURN);
            writeMember.visitMaxs(5, 5);
            writeMember.visitEnd();

            //重载方法：readMember
            MethodVisitor readMember = cw.visitMethod(ACC_PUBLIC, "readMember", "(SLappbox/serialization/BinDeserializer;)V", null, new String[]{"java/lang/Exception"});
            //constructor.visitVarInsn(ALOAD, 2);//参数2
            //constructor.visitMethodInsn(INVOKEVIRTUAL, "appbox/serialization/BinDeserializer", "readShort", "()S",false);
            //readMember.visitInsn(POP);
            readMember.visitInsn(RETURN);
            readMember.visitMaxs(5, 5);
            readMember.visitEnd();
            //完成
            cw.visitEnd();
            code = cw.toByteArray();
        }
        classPool.put(className,code);
        return code;
    }

    /**
     * 添加字段并生成get、set方法
     * @param className
     * @param propertyName
     * @param descriptorClz
     * @return
     * @throws Exception
     */
    public static byte[] addProperty(String className,String propertyName,Class descriptorClz) throws Exception {

        String fullName="com/model/"+className;
        ClassReader cr = new ClassReader(classPool.get(className));
        ClassWriter cw = new ClassWriter(cr, ClassWriter.COMPUTE_MAXS);
        cr.accept(cw, ClassReader.SKIP_DEBUG);
        ///**
        // * 增加属性字段:private String name;
        // */
        FieldVisitor fv = cw.visitField(ACC_PRIVATE, propertyName, Type.getDescriptor(descriptorClz), null, null);
        fv.visitEnd();
        /**
         * 构造setName方法:public void setName(String name);
         */
        MethodVisitor setName = cw.visitMethod(ACC_PUBLIC, "set"+ propertyName, "(" + Type.getDescriptor(descriptorClz) + ")V", null, null);
        setName.visitCode();
        setName.visitVarInsn(ALOAD, 0);
        setName.visitVarInsn(ALOAD, 1);
        setName.visitFieldInsn(PUTFIELD, fullName, propertyName, Type.getDescriptor(descriptorClz));
        setName.visitMaxs(2, 2);
        setName.visitInsn(RETURN);
        setName.visitEnd();
        /**
         * 构造getName方法:public String getName();
         */
        MethodVisitor getName = cw.visitMethod(ACC_PUBLIC, "get"+ propertyName, "()"+Type.getDescriptor(descriptorClz), null, null);
        getName.visitCode();
        getName.visitVarInsn(ALOAD, 0);
        getName.visitFieldInsn(GETFIELD, fullName, propertyName, Type.getDescriptor(descriptorClz));
        getName.visitMaxs(1, 1);
        getName.visitInsn(ARETURN);
        getName.visitEnd();

        cw.visitEnd();
        byte[] code = cw.toByteArray();
        classPool.put(className,code);
        return code;
    }


    //static class AddField extends ClassVisitor {
    //
    //    private String name;
    //    private int access;
    //    private String desc;
    //    private Object value;
    //
    //    private boolean duplicate;
    //
    //    // 构造器
    //    public AddField(ClassVisitor cv, String name, int access, String desc, Object value) {
    //        super(ASM5, cv);
    //        this.name = name;
    //        this.access = access;
    //        this.desc = desc;
    //        this.value = value;
    //    }
    //
    //    @Override
    //    public FieldVisitor visitField(int access, String name, String desc, String signature, Object value) {
    //        if (this.name.equals(name)) {
    //            duplicate = true;
    //        }
    //        return super.visitField(access, name, desc, signature, value);
    //    }
    //
    //    @Override
    //    public void visitEnd() {
    //        if (!duplicate) {
    //            FieldVisitor fv = super.visitField(access, name, desc, null, value);
    //            if (fv != null) {
    //                fv.visitEnd();
    //            }
    //        }
    //        super.visitEnd();
    //    }
    //
    //}
}
