try {
    assert new File(basedir, 'macro/target/classes/QuasiquoteCompat.class').exists()
    assert new File(basedir, 'macro/target/classes/Macros.class').exists()
    assert new File(basedir, 'core/target/classes/Test.class').exists()
    return true
} catch (Throwable e) {
    e.printStackTrace()
    return false
}



