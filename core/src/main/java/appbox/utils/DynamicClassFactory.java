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


    public static Class<?> getClass(String className) throws Exception{
        String fullName="com/model/"+className;
        //自定义ClassLoader

        byte[] code=classPool.get(className);
        if(code==null){
            ClassWriter cw = new ClassWriter(0);
            /**
             * version:指定JAVA的版本
             * access:指定类的权限修饰符
             * name:指定类的全路径名,即类的全限定名中的"."替换成"/"
             * signature:签名
             * superName:继承的父类
             * interfaces:实现的接口(数组)
             */
            cw.visit(V1_8, ACC_PUBLIC, fullName, null, "appbox/data/Entity",null);
            /**
             * 创建该类的构造函数
             * visitMethod:
             * access:指定方法的权限修饰符
             * name:指定方法名,构造函数在字节码中的方法名为"<init>"
             * descriptor:方法描述符(public void add(Integer a,Integer b)-->(II)V)
             * signature:签名
             * exceptions:异常(数组)
             */
            MethodVisitor constructor = cw.visitMethod(ACC_PUBLIC, "<init>", "()V", null, null);
            //将this参数入栈
            constructor.visitVarInsn(ALOAD, 0);
            /**
             * 调用父类无参构造函数
             * visitMethodInsn:
             * opcode:字节码指令(INVOKESPECIAL)
             * owner:被调用方法所属类(指定类的全路径名)
             * name:被调用的方法名
             * descriptor: 被调用方法的方法描述符
             * isInterface:被调用方法的所属类是否是接口
             */
            constructor.visitMethodInsn(INVOKESPECIAL, "appbox/data/Entity", "<init>", "()V",false);
            constructor.visitInsn(RETURN);
            //指定局部变量栈的空间大小
            constructor.visitMaxs(1, 1);
            //构造方法的结束
            constructor.visitEnd();
            //完成
            cw.visitEnd();
            code = cw.toByteArray();
            //可以将其生成class文件保存在磁盘上,或者直接通过classLoad加载
            new File("/home/rick/out/").mkdirs();
            FileOutputStream fos = new FileOutputStream(new File("/home/rick/out/"+fullName+".class"));
            fos.write(code);
            fos.close();
        }
        MyClassLoader classLoader = new MyClassLoader();
        /**
         * name:指定的是加载类的全限制名,即通过"."分隔的
         */
        Class<?> cls = classLoader.defineClassPublic(fullName.replace("/","."), code, 0, code.length);
        classPool.put(className,code);
        return cls;
    }

    public static Class<?> addProperty(String className,String propertyName,Class descriptorClz) throws Exception {
        String classDir="/home/rick/out/com/model/"+className+".class";
        String output = "/home/rick/out/com/model/";
        String fullName="com/model/"+className;
        ClassReader cr = new ClassReader(new FileInputStream(classDir));
        ClassWriter cw = new ClassWriter(cr, ClassWriter.COMPUTE_MAXS);
        ClassVisitor addField = new AddField(cw,
                propertyName,
                Opcodes.ACC_PRIVATE ,
                Type.getDescriptor(descriptorClz),
                ""
        );
        cr.accept(addField, ClassReader.EXPAND_FRAMES);

        ///**
        // * 声明一个成员变量: private String name;
        // * visitField:
        // * access:成员变量的权限修饰符
        // * name:成员变量名
        // * descriptor:成员变量的定义描述符
        // * signature:签名
        // * value:成员变量的初始值
        // */
        //FieldVisitor fv = cw.visitField(ACC_PRIVATE, propertyName, descriptor, null, null);
        //fv.visitEnd();
        ///**
        // * 构造setName方法:public void setName(String name);
        // */
        MethodVisitor setName = cw.visitMethod(ACC_PUBLIC, "set"+ StringUtil.firstUpperCase(propertyName), "(" + Type.getDescriptor(descriptorClz) + ")V", null, null);
        setName.visitCode();
        ////this参数
        setName.visitVarInsn(ALOAD, 0);
        ////传入的name参数
        setName.visitVarInsn(ALOAD, 1);
        ////fullNameType:类的全路径名,即zzz/ddd/ccc/LeakInfo
        setName.visitFieldInsn(PUTFIELD, fullName, propertyName, Type.getDescriptor(descriptorClz));
        setName.visitMaxs(2, 2);
        setName.visitInsn(RETURN);
        setName.visitEnd();
        ///**
        // * 构造getName方法
        // */
        MethodVisitor getName = cw.visitMethod(ACC_PUBLIC, "get"+ StringUtil.firstUpperCase(propertyName), "()"+Type.getDescriptor(descriptorClz), null, null);
        getName.visitCode();
        getName.visitVarInsn(ALOAD, 0);
        getName.visitFieldInsn(GETFIELD, fullName, propertyName, Type.getDescriptor(descriptorClz));
        getName.visitMaxs(1, 1);
        getName.visitInsn(ARETURN);
        getName.visitEnd();

        cw.visitEnd();
        //
        //byte[] code = cw.toByteArray();
        ////可以将其生成class文件保存在磁盘上,或者直接通过classLoad加载
        ////FileOutputStream fos = new FileOutputStream(new File("F:\\LeakInfo.class"));
        ////fos.write(code);
        ////fos.close();
        //
        ////自定义ClassLoader
        //MyClassLoader classLoader = new MyClassLoader();
        ///**
        // * name:指定的是加载类的全限制名,即通过"."分隔的
        // */
        //Class<?> cls = classLoader.defineClassPublic(fullName.replace("/","."), code, 0, code.length);
        //classPool.put(className,code);
        byte[] code = cw.toByteArray();
        File newFile = new File(output, className+".class");
        new FileOutputStream(newFile).write(code);
        MyClassLoader classLoader = new MyClassLoader();
        return classLoader.defineClassPublic(fullName.replace("/","."), code, 0, code.length);
    }

    static class AddField extends ClassVisitor {

        private String name;
        private int access;
        private String desc;
        private Object value;

        private boolean duplicate;

        // 构造器
        public AddField(ClassVisitor cv, String name, int access, String desc, Object value) {
            super(ASM5, cv);
            this.name = name;
            this.access = access;
            this.desc = desc;
            this.value = value;
        }

        @Override
        public FieldVisitor visitField(int access, String name, String desc, String signature, Object value) {
            if (this.name.equals(name)) {
                duplicate = true;
            }
            return super.visitField(access, name, desc, signature, value);
        }

        @Override
        public void visitEnd() {
            if (!duplicate) {
                FieldVisitor fv = super.visitField(access, name, desc, null, value);
                if (fv != null) {
                    fv.visitEnd();
                }
            }
            super.visitEnd();
        }

    }
}
