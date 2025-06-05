from para import *

if __name__ == "__main__":
    # Example data
    data = [1, 3, 5, 7, 9, 2, 4, 6, 8]
    # Execute MapReduce
    result = map_reduce(data, example_map_function, example_reduce_function, max_workers=4)
    print("Final result:", result)

    data = ["1+1=", "hello", "write a python func"]
    def mapf(prompt):
        from deepseek import chat
        return chat(prompt)

    def reducef(mapped_items):
        return mapped_items
    
    result = map_reduce(data, mapf, reducef, max_workers=3)
    print("deepseek result :", result)