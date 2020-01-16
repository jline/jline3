class HelloWorld {
    static void hello(def who) {
       println "hello $who!"
    }       
}

def static main(def _args){
    HelloWorld.hello(!_args ? 'world' : _args[0])
    return 'just testing...'
}
