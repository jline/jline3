class HelloWorld {
    static void hello(def who) {
       println "hello $who!"
    }       
}

def static main(args){
    HelloWorld.hello(!args || args.size() == 0 ? 'world' : args[0])
}
