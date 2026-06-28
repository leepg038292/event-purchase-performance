dependencies {
    implementation(project(":shared"))
    implementation(project(":product:model"))
    implementation(project(":cart:model"))  // ProductQueryPort 반환타입 ProductSummary
}
