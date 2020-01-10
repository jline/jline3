class HelloWorld {
    static void hello(def who) {
       println "hello $who!"
    }       
}

def static main(def args){
    HelloWorld.hello(!args ? 'world' : args[0])
}
