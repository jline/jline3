class HelloWorld {
    static void hello(def who) {
       println "hello $who!"
    }       
}

def static main(args){
    HelloWorld.hello(!args ? 'world' : args[0])
}
