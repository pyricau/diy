package diy

interface Module {
  operator fun <T> get(requestedType: Class<T>): Factory<T>?
}