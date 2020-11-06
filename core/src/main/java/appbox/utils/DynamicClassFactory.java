package appbox.utils;
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

    private static final String CLASS_ROOT_PATH="/home/rick/out/";

    public static byte[] getClassByte(String className) throws Exception{
        String fullName="com/model/"+className;
        //自定义ClassLoader

        byte[] code=classPool.get(className);
        if(code==null){
            ClassWriter cw = new ClassWriter(0);
            cw.visit(V1_8, ACC_PUBLIC, fullName, null, "java/lang/Object",null);
            MethodVisitor constructor = cw.visitMethod(ACC_PUBLIC, "<init>", "()V", null, null);
            constructor.visitVarInsn(ALOAD, 0);
            constructor.visitMethodInsn(INVOKESPECIAL, "java/lang/Object", "<init>", "()V",false);
            constructor.visitInsn(RETURN);
            constructor.visitMaxs(1, 1);
            constructor.visitEnd();
            //完成
            cw.visitEnd();
            code = cw.toByteArray();
            //可以将其生成class文件保存在磁盘上,或者直接通过classLoad加载
            //new File("/home/rick/out/").mkdirs();
            //FileOutputStream fos = new FileOutputStream(new File(CLASS_ROOT_PATH+fullName+".class"));
            //fos.write(code);
            //fos.close();
        }
        //MyClassLoader classLoader = new MyClassLoader();
        ///**
        // * name:指定的是加载类的全限制名,即通过"."分隔的
        // */
        //Class<?> cls = classLoader.defineClassPublic(fullName.replace("/","."), code, 0, code.length);
        classPool.put(className,code);
        return code;
    }

    public static byte[] addProperty(String className,String propertyName,Class descriptorClz) throws Exception {

        String fullName="com/model/"+className;
        ClassReader cr = new ClassReader(classPool.get(className));
        ClassWriter cw = new ClassWriter(cr, ClassWriter.COMPUTE_MAXS);
        //ClassVisitor addField = new AddField(cw,
        //        propertyName,
        //        Opcodes.ACC_PRIVATE ,
        //        Type.getDescriptor(descriptorClz),
        //        ""
        //);
        //cr.accept(addField, ClassReader.EXPAND_FRAMES);

        /**
         * 增加属性字段:private String name;
         */
        FieldVisitor fv = cw.visitField(ACC_PRIVATE, propertyName, Type.getDescriptor(descriptorClz), null, null);
        fv.visitEnd();
        /**
         * 构造setName方法:public void setName(String name);
         */
        MethodVisitor setName = cw.visitMethod(ACC_PUBLIC, "set"+ StringUtil.firstUpperCase(propertyName), "(" + Type.getDescriptor(descriptorClz) + ")V", null, null);
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
        MethodVisitor getName = cw.visitMethod(ACC_PUBLIC, "get"+ StringUtil.firstUpperCase(propertyName), "()"+Type.getDescriptor(descriptorClz), null, null);
        getName.visitCode();
        getName.visitVarInsn(ALOAD, 0);
        getName.visitFieldInsn(GETFIELD, fullName, propertyName, Type.getDescriptor(descriptorClz));
        getName.visitMaxs(1, 1);
        getName.visitInsn(ARETURN);
        getName.visitEnd();

        cw.visitEnd();
        byte[] code = cw.toByteArray();
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
