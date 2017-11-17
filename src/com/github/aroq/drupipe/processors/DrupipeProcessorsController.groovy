package com.github.aroq.drupipe.processors

class DrupipeProcessorsController {

    ArrayList<DrupipeProcessor> processors

    def process(context, object, parent, key = 'params', mode = 'config') {
        if (object instanceof Map) {
            for (DrupipeProcessor processor in processors) {
                object = processor.process(context, object, parent, key, mode)
            }
            for (item in object) {
                object[item.key] = this.process(context, item.value, item.key, key, mode)
            }
        }
        else if (object instanceof List) {
            object = object.collect { this.process(context, it, parent, key, mode) }
        }
        object
    }

}
